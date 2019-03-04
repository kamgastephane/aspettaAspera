package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.protocols.ProgressListener;
import agoda.protocols.ProtocolHandler;
import agoda.storage.MockStorage;
import agoda.storage.Storage;
import agoda.storage.StorageFactory;
import agoda.storage.StorageSupplier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ControllerTest {

    @Test
    public void testThatICandownloadSuccessfullyWithOnThreadWhenIDontAcceptRangeRequest() throws IOException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange = false;
        long size = FileUtils.ONE_KB * 10;
        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_KB, FileUtils.ONE_KB * 16, 10, 3, (int) size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.getMock(FileUtils.ONE_MB, "");
            }
        };

        ProtocolHandler handler = new ProtocolHandler() {

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
                download(url, consumer);
            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {
                consumer.consume(phrase.getBytes(StandardCharsets.UTF_8));

            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return new ResourceInformation(url, acceptRange, size);
            }
        };
        StorageFactory mockStorageFactory = TestUtils.getMockStorageFactory(20 * FileUtils.ONE_MB);
        Controller controller = new Controller(url, null, configuration, 10, BasicSegmentCalculator.getInstance(), handler,
                mockStorageFactory);
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
//        File f = new File(directory,"5MB.zip");

//        RandomAccessFile raf = new RandomAccessFile(f,"r");
        byte[] expected = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[expected.length];
        MockStorage mockStorage = (MockStorage) controller.controllerStatus.getStorage(0).get();
        ByteArrayOutputStream outputStream = mockStorage.getbuffer();
        ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
        while (stream.read(result) != -1) {
            Assert.assertArrayEquals(result, expected);
        }

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex()).get();
            assert storage.isClosed();

        }
        stream.close();
        outputStream.close();


    }



    @Test
    public void testThatIfASegmentFailIFailGracefullyWhenDownloadingMultipleSegments() throws IOException, InterruptedException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange = true;
        long size = FileUtils.ONE_MB * 10;

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
                if (from == 0) {
                    firstSegment = true;
                }
                //after the third request i throw an exception only on one of the two threads running
                if (count > 3 && firstSegment) {
                    throw new DownloadException("i faked a download exception", new RuntimeException("i faked a download exception"));
                }
                count++;
                try {
                    TimeUnit.SECONDS.sleep(1);
                    consumer.consume(phrase.getBytes(StandardCharsets.UTF_8));

                } catch (InterruptedException e) {
                    throw new DownloadException("", e);
                }

            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {
                download(url, 0, 0, consumer);
            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return new ResourceInformation(url, acceptRange, size);

            }


        };
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_MB, FileUtils.ONE_MB * 2, 10, 3, (int) size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.getMock(FileUtils.ONE_MB, "");
            }
        };
        Controller controller = new Controller(url, null, configuration, 2,
                BasicSegmentCalculator.getInstance(), handler, TestUtils.getMockStorageFactory(size));

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
            StorageSupplier storage = controller.controllerStatus.getStorage(segment.getSegmentIndex());
            if (storage != null && storage.isInit()) {
                Assert.assertTrue(storage.get().isClosed());
            }

        }

    }

    @Test
    public void testThatTheDownloadIsDoneWithOneThreadSuccessFullyIfICannotProvideTheSizeOfTheFile() throws IOException {
        String phrase = "the cat is on the table";
        //with acceptRange at false i can only download with one thread
        boolean acceptRange = true;
        long size = FileUtils.ONE_MB * 10;

        String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        Configuration configuration = new Configuration() {
            @Override
            public DownloaderConfiguration getDownloaderConfiguration() {
                return TestUtils.getMockDownloaderConfiguration(FileUtils.ONE_KB, FileUtils.ONE_KB * 16, 10, 3, (int) size);
            }

            @Override
            public StorageConfiguration getStorageConfiguration() {
                return TestUtils.getMock(FileUtils.ONE_MB, "");
            }
        };

        ProtocolHandler handler = new ProtocolHandler() {
            int count = 0;

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
                download(url, consumer);

            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {
                if (count < 10) {
                    count++;
                    consumer.consume(phrase.getBytes(StandardCharsets.UTF_8));
                    return;
                }
                consumer.consume(new byte[0]);

            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return new ResourceInformation(url, acceptRange, 0);
            }


        };
        Controller controller = new Controller(url, null, configuration, 10, BasicSegmentCalculator.getInstance(), handler,
                TestUtils.getMockStorageFactory(size));
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


        byte[] expected = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[expected.length];

        MockStorage mockStorage = (MockStorage) controller.controllerStatus.getStorage(0).get();
        InputStream stream = mockStorage.getbuffer().toInputStream();
        while (stream.read(result) != -1) {
            Assert.assertArrayEquals(result, expected);
        }

        List<Segment> segments = controller.controllerStatus.getSegmentRelatedTo(url);
        for (Segment segment : segments) {
            Storage storage = controller.controllerStatus.getStorage(segment.getSegmentIndex()).get();
            assert storage.isClosed();

        }
    }







}