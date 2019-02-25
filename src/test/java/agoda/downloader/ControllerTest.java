package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.protocols.HttpProtocolHandler;
import agoda.protocols.ProtocolHandler;
import agoda.storage.Storage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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
                return TestUtils.get(FileUtils.ONE_KB,FileUtils.ONE_KB*16,10,3);
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
            public byte[] download(String url, long from, long to) throws DownloadException {
                return download(url,from);

            }

            @Override
            public byte[] download(String url, long from) throws DownloadException {
                return phrase.getBytes(StandardCharsets.UTF_8);
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
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            assert storage.isClosed();

        }


        FileUtils.deleteQuietly(TestUtils.getTempDirectory());


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
            public byte[] download(String url, long from, long to) throws DownloadException {
                return download(url,from);

            }

            @Override
            public byte[] download(String url, long from) throws DownloadException {
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
                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }
                return phrase.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public DownloadInformation getInfo(String url) {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,FileUtils.ONE_MB*2,10,3);
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
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            assert storage.isClosed();
            File f = new File(storage.getFileName());
            assert !f.exists();
        }



        FileUtils.deleteQuietly(TestUtils.getTempDirectory());



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
                return TestUtils.get(FileUtils.ONE_KB,FileUtils.ONE_KB*16,10,3);
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
            public byte[] download(String url, long from, long to) throws DownloadException {
                return download(url,from);

            }

            @Override
            public byte[] download(String url, long from) throws DownloadException {
                if(count<10){
                    count++;
                    return phrase.getBytes(StandardCharsets.UTF_8);
                }
                return null;
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


        File f = new File(controller.controllerStatus.getStorage(0).getFileName());

        RandomAccessFile raf = new RandomAccessFile(f,"r");
        byte[] expected = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[expected.length];

        while (raf.read(result)!=-1)
        {
            Assert.assertArrayEquals(result,expected);
        }

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            assert storage.isClosed();

        }


        FileUtils.deleteQuietly(TestUtils.getTempDirectory());
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
            public byte[] download(String url, long from, long to) throws DownloadException {
                return download(url,from);

            }

            @Override
            public byte[] download(String url, long from) throws DownloadException {

                return phrase.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public DownloadInformation getInfo(String url) {
                return new DownloadInformation(url,acceptRange,size);
            }
        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.get(FileUtils.ONE_MB,FileUtils.ONE_MB*2,2,3);
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
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            assert storage.isClosed();
            File f = new File(storage.getFileName());

            if(segment.getSegmentIndex()==0)
            {
                assert f.exists();
            }else {
                assert !f.exists();
            }
        }
        assert controller.getTotalSaved() == size;
        FileUtils.deleteQuietly(TestUtils.getTempDirectory());

    }
    public void testThatIfTheDownloadFailBecauseOfAnExceptionIFailGracefully()
    {

    }
    public void testThatIfTheDownloadFailBecauseOfIStopReceivingDataIfailGracefully()
    {

    }
    @Test
    public void generateStorageUsingSegmentWithBothGoodAndBadUrlsAndVerifyTheStorageIsOnlyForTheGoodUrls() {
        File file = null;
        try {

             file = Files.createTempDirectory("agoda").toFile();

            String goodUrl1 = "http://www.agoda.com";
            String badUrl1 = "qwertyuioplkjjhgfdsa";
            String goodUrl2 = "http://www.booking.com";

            Segment segment1 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(badUrl1).createSegment();
            Segment segment2 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(badUrl1).createSegment();
            Segment segment3 = new Segment.SegmentBuilder().setSegmentIndex(3).setSrcUrl(badUrl1).createSegment();
            Segment segment4 = new Segment.SegmentBuilder().setSegmentIndex(4).setSrcUrl(badUrl1).createSegment();

            Segment segment5 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(goodUrl1).createSegment();
            Segment segment6 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(goodUrl1).createSegment();
            Segment segment7 = new Segment.SegmentBuilder().setSegmentIndex(3).setSrcUrl(goodUrl1).createSegment();
            Segment segment8 = new Segment.SegmentBuilder().setSegmentIndex(4).setSrcUrl(goodUrl1).createSegment();


            Segment segment10 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(goodUrl2).createSegment();
            Segment segment11 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(goodUrl2).createSegment();


            ProtocolHandler handler = new HttpProtocolHandler();
            //i should have storage created only for the two goods urls
            File finalFile = file;
            Configuration config = new Configuration() {
                @Override
                public DownloaderConfiguration getDownloaderConfiguration() {
                    return null;
                }

                @Override
                public StorageConfiguration getStorageConfiguration() {
                    return new StorageConfiguration() {
                        @Override
                        public int getOutputStreamBufferSize() {
                            return 64;
                        }



                        @Override
                        public String getDownloadFolder() {
                            return finalFile.getAbsolutePath();


                        }
                    };
                }
            };

            Controller controller = new Controller(goodUrl1,config,10,BasicSegmentCalculator.getInstance(),handler);

            Map<String, List<Storage>> storage = controller.initStorage(Arrays.asList(segment1, segment2, segment3, segment4, segment5, segment6, segment7, segment8, segment10, segment11));
            assertEquals(storage.size(),2);
            assert storage.keySet().contains(goodUrl1);
            assert storage.keySet().contains(goodUrl2);
            assertEquals(storage.get(goodUrl1).size(),4);
            assertEquals(storage.get(goodUrl2).size(),2);


        }catch (IOException ignored) {
        }finally {
            if (file!=null)file.delete();
        }



        }

}