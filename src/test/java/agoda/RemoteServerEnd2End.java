package agoda;

import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.downloader.SimpleSegmentCalculator;
import agoda.downloader.Controller;
import agoda.downloader.DownloadException;
import agoda.downloader.TestUtils;
import agoda.protocols.ProtocolHandler;
import agoda.protocols.ProtocolHandlerFactory;
import agoda.storage.StorageFactory;
import agoda.storage.StorageFactoryImpl;
import agoda.storage.TestStorageUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashMap;

public class RemoteServerEnd2End {


    //This can be used for end 2 end http://speedtest.tele2.net => 90.130.70.73

    private  File tempDirectory;
    @Before
    public void setUp() throws Exception {
        tempDirectory = TestStorageUtils.getTempDirectory();
    }
    @After
    public void tearDown() throws Exception {


        FileUtils.deleteQuietly(tempDirectory);
    }

    @Test
    public void testDownloadEnd2EndWithFTP() throws DownloadException, InterruptedException, IOException {
        //ftp://speedtest.tele2.net/100MB.zip
        //this test is really messy because of connection error
        String url = "ftp://speedtest.tele2.net/100MB.zip";

        HashMap<String,String> connectionParams = new HashMap<>();
        connectionParams.put("port","21");

        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        DownloaderConfiguration mockDownloaderConfiguration = TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB, FileUtils.ONE_MB * 100, 1, 2, (int) (FileUtils.ONE_MB * 100),false);
        StorageFactory storageFactory = StorageFactoryImpl.getInstance();

        StorageConfiguration storageConfiguration = new StorageConfiguration() {
            @Override
            public int getOutputStreamBufferSize() {
                return (int) (FileUtils.ONE_MB *2);
            }

            @Override
            public String getDownloadFolder() {
                return tempDirectory.getAbsolutePath();
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return mockDownloaderConfiguration;
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return storageConfiguration;
            }
        };
        SimpleSegmentCalculator segmentCalculator = SimpleSegmentCalculator.getInstance();
        Controller controller = new Controller(url, connectionParams,configuration,1,
                segmentCalculator,protocolHandler,storageFactory);
        controller.setup();
        controller.run();

        File zipFile = new File(tempDirectory.getAbsolutePath(),"100MB.zip");
        byte[] bytes = FileUtils.readFileToByteArray(zipFile);


        Assert.assertEquals(bytes.length, 100 * FileUtils.ONE_MB);

    }

    @Test
    public void testTheMemoryDoesNotGetOutOfEndUsingRemoteServer() throws DownloadException, InterruptedException, IOException {

        String url = "http://speedtest.tele2.net/1GB.zip";
        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        DownloaderConfiguration mockDownloaderConfiguration = TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB *50, FileUtils.ONE_MB * 100, 5, 2, (int) (FileUtils.ONE_MB * 2),false);
        StorageFactory storageFactory = StorageFactoryImpl.getInstance();

        StorageConfiguration storageConfiguration = new StorageConfiguration() {
            @Override
            public int getOutputStreamBufferSize() {
                return (int) (FileUtils.ONE_MB *2);
            }

            @Override
            public String getDownloadFolder() {
                return tempDirectory.getAbsolutePath();
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return mockDownloaderConfiguration;
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return storageConfiguration;
            }
        };
        SimpleSegmentCalculator segmentCalculator = SimpleSegmentCalculator.getInstance();
        Controller controller = new Controller(url, new HashMap<>(),configuration,10,
                segmentCalculator,protocolHandler,storageFactory);
        controller.setup();
        controller.run();


        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long l = heapMemoryUsage.getUsed() / FileUtils.ONE_MB;
        System.out.println("heap size in MB: "+l);
        assert l < 100;

    }



}
