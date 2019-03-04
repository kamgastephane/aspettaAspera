package agoda.protocols;


import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.downloader.*;
import agoda.storage.StorageFactory;
import agoda.storage.StorageFactoryImpl;
import agoda.storage.TestStorageUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpProtocolHandlerIT {


    private  File tempDirectory;
    private Server server;
    @Before
    public void setUp() throws Exception {

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.setConnectors(new Connector[]{connector});

        ServletContextHandler root = new ServletContextHandler(server, "/");


        ServletHolder staticHolder = new ServletHolder("static", DefaultServlet.class);

        staticHolder.setInitParameter("resourceBase",this.getClass().getClassLoader().getResource("").getPath());
        staticHolder.setInitParameter("dirAllowed","true");
        staticHolder.setInitParameter("pathInfoOnly","true");
        root.addServlet(staticHolder,"/static/*");


        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        holderPwd.setInitParameter("dirAllowed","true");
        root.addServlet(holderPwd,"/");

        server.start();
        tempDirectory = TestStorageUtils.getTempDirectory();


    }


    @Test
    public void testGetInfo() throws DownloadException, InterruptedException {

        String url = server.getURI().toString()+"static/music.mp3";
        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        Assert.assertNotNull(protocolHandler);


        ResourceInformation info = protocolHandler.getInfo(url, null);
        Assert.assertTrue(info.isAcceptRange());
        Assert.assertTrue(info.getSize()>0);
    }
    @Test
    public void testDownload() throws DownloadException, InterruptedException, IOException {

        String url = server.getURI().toString()+"static/music.mp3";
        long size = 14664053L;
        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Assert.assertNotNull(protocolHandler);
        List<Integer> read = new ArrayList<>();

        protocolHandler.download(url, buffer -> {
                if (buffer!=ProtocolHandler.EOF)
                {
                    read.add(buffer.length);
                    try {
                        stream.write(buffer);
                    } catch (IOException e) {
                        throw new DownloadException("",e);
                    }
                }
                return true;
        });

        Assert.assertEquals(read.stream().mapToInt(x -> x).sum(), size);
        Assert.assertEquals(stream.size(), size);

        File musicFile = new File(this.getClass().getClassLoader().getResource("music.mp3").getFile());
        byte[] musicBytes = FileUtils.readFileToByteArray(musicFile);

        Assert.assertArrayEquals(musicBytes,stream.toByteArray());
        stream.close();
    }
    @Test
    public void testDownloadEnd2EndWithHttp() throws DownloadException, InterruptedException, IOException {

        String url = server.getURI().toString()+"static/music.mp3";

        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        DownloaderConfiguration mockDownloaderConfiguration = TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB, FileUtils.ONE_MB * 2, 2, 2, (int) (FileUtils.ONE_KB * 100));
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
        BasicSegmentCalculator segmentCalculator = BasicSegmentCalculator.getInstance();
        Controller controller = new Controller(url, new HashMap<>(),configuration,2,
                segmentCalculator,protocolHandler,storageFactory);
        controller.setup();
        controller.run();
        System.out.println(tempDirectory.getAbsolutePath());

        File musicFile = new File(tempDirectory.getAbsolutePath(),"music.mp3");
        byte[] musicBytes = FileUtils.readFileToByteArray(musicFile);

        File original = new File(this.getClass().getClassLoader().getResource("music.mp3").getPath());
        byte[] originalBytes = FileUtils.readFileToByteArray(original);

        Assert.assertArrayEquals(musicBytes,originalBytes);
    }


    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
        FileUtils.deleteQuietly(tempDirectory);

    }
}