package aspettaaspera.downloader;

import aspettaaspera.configuration.Configuration;
import aspettaaspera.configuration.DownloaderConfiguration;
import aspettaaspera.configuration.StorageConfiguration;
import aspettaaspera.protocols.ProgressListener;
import aspettaaspera.protocols.ProtocolHandler;
import aspettaaspera.storage.Storage;
import aspettaaspera.storage.StorageFactoryImpl;
import aspettaaspera.storage.StorageSupplier;
import aspettaaspera.storage.TestStorageUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ControllerIT {

    @Test
    public void testThatTheDownloadIsDoneWithMultipleThreadAndTheJoinIsSuccessfull()
    {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = true;
        long size = FileUtils.ONE_MB * 3;
        File directory = TestStorageUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            boolean firstSegment = false;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
                download(url,consumer);

            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {

                consumer.consume( phrase.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return new ResourceInformation(url,acceptRange,size);
            }


        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB,FileUtils.ONE_MB*2,2,3,(int)size,false);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.getMockStorageConfiguration(FileUtils.ONE_MB,directory);
            }
        };
        Controller controller = new Controller(url,null,configuration,2,
                SimpleSegmentCalculator.getInstance(),handler,
                StorageFactoryImpl.getInstance());

        //i expect the download to be done with 2 thread
        //i have 10mb with max concurrency 2 => 5 mb by thread but i do have a limit on the segment size
        //with max segment size being at 2M i will have 10mb/2MB segments using the basicSegmentCalculator
        controller.setup();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) controller.pool;
        assert threadPoolExecutor.getCorePoolSize() == 2;
        controller.run();


        assert controller.controllerStatus.getIdles().size() == 0;
        assert controller.controllerStatus.getDownloading().size() == 0;

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            assert segment.getStatus() == DownloadStatus.FINISHED;
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex()).get();
            assert storage.isClosed();
            File f = new File(storage.getFileName());

            if(segment.getSegmentIndex()==0)
            {
                assert f.exists();
            }else {
                assert !f.exists();
            }
        }
        assert controller.getSegmentSizeSaved() == size;
        FileUtils.deleteQuietly(directory);

    }

    @Test
    public void testThatIfTheDownloadFailBecauseOfIStopReceivingDataIfailGracefully()
    {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = true;
        long size = FileUtils.ONE_MB * 10;
        File directory = TestStorageUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            boolean firstSegment = false;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
                try {
                    if (from == 0)
                    {
                        firstSegment = true;
                    }
                    //after the third request i throw an exception only on one of the two threads running
                    if(count > 3 && firstSegment)
                    {
                        consumer.consume(null);
                    }
                    count++;

                    TimeUnit.SECONDS.sleep(1);

                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }
            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {


            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return new ResourceInformation(url,acceptRange,size);
            }


        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB,FileUtils.ONE_MB*2,2,3, (int) size,false);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.getMockStorageConfiguration(FileUtils.ONE_MB,directory);
            }
        };
        Controller controller = new Controller(url,null,configuration,2,
                SimpleSegmentCalculator.getInstance(),handler,
                StorageFactoryImpl.getInstance());


        controller.setup();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) controller.pool;
        assert threadPoolExecutor.getCorePoolSize() == 2;
        controller.run();


        assert controller.controllerStatus.getIdles().size() == 0;
        assert controller.controllerStatus.getDownloading().size() == 0;

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            assert segment.getStatus() == DownloadStatus.ERROR;
            StorageSupplier storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            if(storage!=null && storage.isInit())
            {
                Assert.assertTrue(storage.get().isClosed());
                File f = new File(storage.get().getFileName());
                assert !f.exists();

            }

        }
    }
}
