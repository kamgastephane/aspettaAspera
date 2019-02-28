package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.protocols.ChunkConsumer;
import agoda.protocols.ProtocolHandler;
import agoda.storage.LazyStorage;
import agoda.storage.Storage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ControllerTest {

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(TestUtils.getTempDirectory());

    }
    @Test
    public void testThatICandownloadSuccessfullyWithOnThreadWhenIDontAcceptRangeRequest() throws IOException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = false;
        long size = FileUtils.ONE_MB * 1;
        File directory = TestUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_KB,FileUtils.ONE_KB*16,10,3, (int) size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,directory);
            }
        };

        ProtocolHandler handler = new ProtocolHandler() {

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to, ChunkConsumer consumer) throws DownloadException {
                download(url,consumer);

            }

            @Override
            public void download(String url, ChunkConsumer consumer) throws DownloadException {
                try {
                    consumer.consume( phrase.getBytes(StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }

            }


            @Override
            public DownloadInformation getInfo(String url) throws DownloadException {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Controller controller = new Controller(url,configuration,10,BasicSegmentCalculator.getInstance(),handler);
        //i expect the download to be done with one thread
        //i have 10mb with max concurrency 10 => 1 mb by thread but i do not have range request so no concurrency will be on
        //with max segment size being at 16kb i will have 10mb/16kb segments using the basicSegmentCalculator
        controller.setup();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) controller.pool;
        assert threadPoolExecutor.getCorePoolSize() == 1;
        controller.run();

        assert controller.controllerStatus.areAllDownloadsRelatedToFinished(url);
        assert controller.controllerStatus.getIdles().size() == 0;
        assert controller.controllerStatus.getDownloading().size() == 0;

        controller.controllerStatus.getStorage(0);
        File f = new File(directory,"5MB.zip");

        RandomAccessFile raf = new RandomAccessFile(f,"r");
        byte[] expected = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[expected.length];

        while (raf.read(result)!=-1)
        {
            Assert.assertArrayEquals(result,expected);
        }

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex()).get();
            assert storage.isClosed();

        }


        FileUtils.deleteQuietly(directory);


    }
    @Test
    public void testThatIfASegmentFailIFailGracefullyWhenDownloadingMultipleSegments() throws IOException, InterruptedException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = true;
        long size = FileUtils.ONE_MB * 10;
        File directory = TestUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            boolean firstSegment = false;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to,ChunkConsumer consumer) throws DownloadException {
                if (from == 0)
                {
                    firstSegment = true;
                }
                //after the third request i throw an exception only on one of the two threads running
                if(count > 3 && firstSegment)
                {
                    throw new DownloadException("i faked a download exception",new RuntimeException("i faked a download exception"));
                }
                count++;
                try {
                    TimeUnit.SECONDS.sleep(1);
                    consumer.consume(phrase.getBytes(StandardCharsets.UTF_8));

                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }

            }

            @Override
            public void download(String url, ChunkConsumer consumer) throws DownloadException {
                download(url,0,0,consumer);
            }

            @Override
            public DownloadInformation getInfo(String url) {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,FileUtils.ONE_MB*2,10,3,(int)size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,directory);
            }
        };
        Controller controller = new Controller(url,configuration,2,BasicSegmentCalculator.getInstance(),handler);

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
            assert segment.getStatus() == DownloadStatus.ERROR;
            LazyStorage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            if(storage!=null && storage.isInit())
            {
                Assert.assertTrue(storage.get().isClosed());
                File f = new File(storage.get().getFileName());
                assert !f.exists();
            }

        }



        FileUtils.deleteQuietly(directory);



    }
    @Test
    public void testThatTheDownloadIsDoneWithOneThreadSuccessFullyIfICannotProvideTheSizeOfTheFile() throws IOException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = true;
        long size = FileUtils.ONE_MB * 10;
        File directory = TestUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_KB,FileUtils.ONE_KB*16,10,3,(int)size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,directory);
            }
        };

        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to,ChunkConsumer consumer) throws DownloadException {
                download(url,consumer);

            }

            @Override
            public void download(String url, ChunkConsumer consumer) throws DownloadException {
                if(count<10){
                    count++;
                    try {
                        consumer.consume( phrase.getBytes(StandardCharsets.UTF_8));
                        consumer.consume(new byte[0]);

                    } catch (InterruptedException e) {
                        throw new DownloadException("",e);
                    }
                }
            }

            @Override
            public DownloadInformation getInfo(String url) throws DownloadException {
                return new DownloadInformation(url,acceptRange,0);
            }
        };
        Controller controller = new Controller(url,configuration,10,BasicSegmentCalculator.getInstance(),handler);
        //i expect the download to be done with one thread
        //i have 10mb with max concurrency 10 => 1 mb by thread but i do not have range request so no concurrency will be on
        //with max segment size being at 16kb i will have 10mb/16kb segments using the basicSegmentCalculator
        controller.setup();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) controller.pool;
        assert threadPoolExecutor.getCorePoolSize() == 1;
        controller.run();

        assert controller.controllerStatus.areAllDownloadsRelatedToFinished(url);
        assert controller.controllerStatus.getIdles().size() == 0;
        assert controller.controllerStatus.getDownloading().size() == 0;


        File f = new File(controller.controllerStatus.getStorage(0).get().getFileName());

        RandomAccessFile raf = new RandomAccessFile(f,"r");
        byte[] expected = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[expected.length];

        while (raf.read(result)!=-1)
        {
            Assert.assertArrayEquals(result,expected);
        }

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex()).get();
            assert storage.isClosed();

        }


        FileUtils.deleteQuietly(directory);
    }
    @Test
    public void testThatTheDownloadIsDoneWithMultipleThreadAndTheJoinIsSuccessfull()
    {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange  = true;
        long size = FileUtils.ONE_MB * 3;
        File directory = TestUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            boolean firstSegment = false;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to,ChunkConsumer consumer) throws DownloadException {
                download(url,consumer);

            }

            @Override
            public void download(String url,ChunkConsumer consumer) throws DownloadException {

                try {
                    consumer.consume( phrase.getBytes(StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }
            }

            @Override
            public DownloadInformation getInfo(String url) {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,FileUtils.ONE_MB*2,2,3,(int)size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,directory);
            }
        };
        Controller controller = new Controller(url,configuration,2,BasicSegmentCalculator.getInstance(),handler);

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
        File directory = TestUtils.getTempDirectory();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;
            boolean firstSegment = false;
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to,ChunkConsumer consumer) throws DownloadException {
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

                    consumer.consume(phrase.getBytes(StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }
            }

            @Override
            public void download(String url, ChunkConsumer consumer) throws DownloadException {
                download(url,consumer);

            }

            @Override
            public DownloadInformation getInfo(String url) {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,FileUtils.ONE_MB*2,10,3, (int) size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,directory);
            }
        };
        Controller controller = new Controller(url,configuration,2,BasicSegmentCalculator.getInstance(),handler);


        controller.setup();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) controller.pool;
        assert threadPoolExecutor.getCorePoolSize() == 2;
        controller.run();


        assert controller.controllerStatus.getIdles().size() == 0;
        assert controller.controllerStatus.getDownloading().size() == 0;

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            assert segment.getStatus() == DownloadStatus.ERROR;
            LazyStorage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            if(storage!=null && storage.isInit())
            {
                Assert.assertTrue(storage.get().isClosed());
                File f = new File(storage.get().getFileName());
                assert !f.exists();

            }

        }



        FileUtils.deleteQuietly(directory);

    }


}