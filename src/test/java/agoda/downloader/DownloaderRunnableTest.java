package agoda.downloader;

import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ProgressListener;
import agoda.protocols.ProtocolHandler;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class DownloaderRunnableTest {
    private static final Logger logger = LogManager.getLogger();

    private Random random;

    @Before
    public void setUp() throws Exception {
        random = new Random();

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testTheDownloadRunnableStopWhenItReceiveAnInterruptFromTheController() throws InterruptedException, ExecutionException {
        ProtocolHandler mockHandler = new ProtocolHandler() {
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
                try {
                    TimeUnit.SECONDS.sleep(10);
                    byte[] buffer = new byte[1];
                    random.nextBytes(buffer);
                    consumer.consume(buffer);
                } catch (InterruptedException e) {
                    logger.error("i triggered an exception after 1 seconds",e);
                    throw new DownloadException("",new RuntimeException("",new RuntimeException("",e)));
                }

            }


            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };
        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setEndPosition(1024)
                .createSegment();
        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();

        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);
        TimeUnit.SECONDS.sleep(1);
        future.cancel(true);
        TimeUnit.SECONDS.sleep(2);

        ResultMessage message = queue.take();

        assert message.getStatus().equals(DownloadStatus.ERROR);


    }
    @Test
    public void testThatTheDownloadEventuallyStopAfterTheMaxRetryTimeIfTheProtocolNeverConsume() throws InterruptedException {
        final int[] tried = {0};
        ProtocolHandler mockHandler = new ProtocolHandler() {
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
                tried[0]++;

            }


            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };
        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setEndPosition(1024)
                .setMaxRetry(3)
                .createSegment();
        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();

        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        service.submit(runnable);

        ResultMessage message = queue.take();
        assert message.getStatus().equals(DownloadStatus.ERROR);
        assert tried[0] == 3;


    }

    @Test
    public void testTheDownloadRateIsCloseToWhatIsExpected() throws InterruptedException, ExecutionException {
        ProtocolHandler mockHandler = new ProtocolHandler() {
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
                try {
                    TimeUnit.SECONDS.sleep(1);
                    byte[] buffer = new byte[1024];
                    random.nextBytes(buffer);
                    consumer.consume(buffer);
                } catch (InterruptedException e) {
                    logger.error("i triggered an exception after 1 seconds",e);
                    throw new DownloadException("",new RuntimeException("",new RuntimeException("",e)));
                }

            }


            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };
        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setEndPosition((1024 *30)-1)
                .createSegment();
        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();

        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        service.submit(runnable);
        int count = 0;
        TimeUnit.SECONDS.sleep(40);

        ResultMessage message = null;
        while (count < 30)
        {
            message = queue.take();
            count++;
        }
        assert message.getStatus().equals(DownloadStatus.FINISHED);

        //as i am downloading at approx 8kbytes/s, i expect a precision at 0.1%
        System.out.println(message.getRate());
        assert Math.abs((message.getRate()-1)) < 0.01;





    }

    @Test
    public void testItStopWhenNullIsReceivedForMoreThanMaxRetrySize() throws InterruptedException {
        int maxRetry = 5;
        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setMaxRetry(maxRetry)
                .setEndPosition(1024)
                .createSegment();

        ProtocolHandler mockHandler = new ProtocolHandler() {
            int count = 0;

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
                //we return data the two first time and then we start returning 0 bytes
                    count++;
                if (count >= 3 && count < 8) {

                        consumer.consume(null);

                } else {
                    byte[] buffer = new byte[1];
                    random.nextBytes(buffer);
                    consumer.consume(buffer);
                }
            }



            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };


        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();
        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);
        TimeUnit.SECONDS.sleep(2);


        ResultMessage message = queue.take();
        assert message.getContent().length > 0;
        message = queue.take();
        assert message.getContent().length > 0;

        //after the second message i should not receive anything until the error
        message = queue.take();
        assert message.getStatus() == DownloadStatus.ERROR;



    }

    @Test
    public void testItStopWhenAnExceptionIsThrownMoreThanMaxRetrySize() throws InterruptedException {
        int maxRetry = 5;
        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setMaxRetry(maxRetry)
                .setEndPosition(1024)
                .createSegment();

        ProtocolHandler mockHandler = new ProtocolHandler() {
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
                //we return data the two first time and then we start throwing exceptions
                    count++;
                    if (count >= 3 && count < 8) {
                        throw new DownloadException("", new Exception());
                    } else {
                        byte[] buffer = new byte[1];
                        random.nextBytes(buffer);
                        consumer.consume(buffer);
                    }

            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };


        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();
        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);
        TimeUnit.SECONDS.sleep(2);


        ResultMessage message = queue.take();
        assert message.getContent().length > 0;
        message = queue.take();
        assert message.getContent().length > 0;

        //after the second message i should not receive anything until the error
        message = queue.take();
        assert message.getStatus() == DownloadStatus.ERROR;

    }

    @Test
    public void testTheCorrectDownloadedDataAreSentAndReceivedOnTheControllerQueue() throws InterruptedException {
        int maxRetry = 5;
        String phrase = "the cat is under the table";

        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setMaxRetry(maxRetry)
                .setEndPosition(phrase.length()-1)
                .createSegment();

        ProtocolHandler mockHandler = new ProtocolHandler() {
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
            public void  download(String url, ProgressListener consumer) throws DownloadException {
                //we return data from our phrase
                   char c = phrase.charAt(count);
                try {
                    //we sleep a little bit to have a decent rate value
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    throw new DownloadException("",e);
                }
                byte[] buffer = Character.toString(c).getBytes();
                   count++;
                   consumer.consume( buffer);
            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };
        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();
        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);
        TimeUnit.SECONDS.sleep(2);


        int messageExpected = phrase.length();
        for (int i = 0; i < messageExpected-1; i++) {
            ResultMessage message = queue.take();
            Assert.assertEquals(message.getStatus(), DownloadStatus.DOWNLOADING);
            char c = phrase.charAt(i);
            byte[] buffer = Character.toString(c).getBytes();
            Assert.assertArrayEquals(buffer, message.getContent());
            System.out.println(message.getRate());
        }
        ResultMessage message = queue.take();
        Assert.assertEquals(message.getStatus(), DownloadStatus.FINISHED);
        assert message.getRate() > 0;

        char c = phrase.charAt(messageExpected-1);
        byte[] buffer = Character.toString(c).getBytes();
        Assert.assertArrayEquals(buffer, message.getContent());

        TimeUnit.SECONDS.sleep(1);
        assert service.getActiveCount() == 0;


    }

    @Test
    public  void testThatEvenIfTheProtocollSendDataBiggerThanTheSegmentSize_IcanTruncateItCorrectly() throws InterruptedException {
        int maxRetry = 5;
        String phrase = "the cat is under the table, lorem ipsum dolor doremifasolasidomilosifamiredo doremifasolasidomilosifamiredo doremifasolasidomilosifamiredo" ;
        int chunkSize = 2*(int)(FileUtils.ONE_KB);

        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setMaxRetry(maxRetry)
                .setEndPosition(5*(int)(FileUtils.ONE_KB)-1)
                .createSegment();

        //my protocol send block of chunkSize
        ProtocolHandler mockHandler = new ProtocolHandler() {

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public void download(String url, ProgressListener consumer) throws DownloadException {
                download(url, consumer);
            }

            @Override
            public void download(String url, long from,long to, ProgressListener consumer) throws DownloadException {
                //we return data from our phrase
                    byte[] buffer = new byte[chunkSize];
                    int copied = 0;
                    byte[] content = phrase.getBytes(StandardCharsets.UTF_8);
                    while (copied+content.length<chunkSize)
                    {
                        System.arraycopy(content,0,buffer,copied,content.length);
                        copied+=content.length;
                    }
                    System.arraycopy(content,0,buffer,copied,chunkSize-copied);

                    consumer.consume(buffer);

            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }
        };

        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();
        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);

        //i should receive three blocks of 30kb and 1 blocks of 10kbs
        for (int i = 0; i < 2; i++) {
            ResultMessage message = queue.take();
            assert message.getStatus() == DownloadStatus.DOWNLOADING;

            Assert.assertEquals(chunkSize, message.getContent().length);
        }
        ResultMessage message = queue.take();
        assert message.getStatus() == DownloadStatus.FINISHED;
        Assert.assertEquals(FileUtils.ONE_KB , message.getContent().length);
        String result = new String(message.getContent(),StandardCharsets.UTF_8);
        assert result.startsWith(phrase);

    }

    @Test
    public void testICanSendProperDataOneOutOf_MAXRETRY_TimeWithoutIssue() throws InterruptedException {
        int maxRetry = 2;

        Segment segment = new Segment.SegmentBuilder()
                .setSegmentIndex(0)
                .setRequestRange(1)
                .setMaxRetry(maxRetry)
                .setEndPosition(1024)
                .createSegment();
        int length = 23;


        ProtocolHandler mockHandler = new ProtocolHandler() {
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
                //we return data from our phrase
                    if (count < length) {
                        byte[] buffer;
                        if (count % maxRetry == 0) {
                            buffer = new byte[1];
                            random.nextBytes(buffer);
                        } else {
                            buffer = null;
                        }
                        count++;
                        consumer.consume(buffer);
                    }else {
                        consumer.consume(null);

                    }
                    //i send an extra bad data here for the n plus 1 time and it should trigger an error as the max retry is 2


            }

            @Override
            public ResourceInformation getInfo(String url, Map<String, String> parameters) throws DownloadException {
                return null;
            }


        };
        LinkedBlockingQueue<ResultMessage> queue = new LinkedBlockingQueue<>();
        DownloaderRunnable runnable = new DownloaderRunnable(segment, mockHandler, queue);

        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        Future future = service.submit(runnable);
        TimeUnit.SECONDS.sleep(2);

        int numberOfGoodMessagesBeforeError = ((length%maxRetry)>0?1:0)+ (length - (length % maxRetry))/maxRetry;
        for (int i = 0; i < numberOfGoodMessagesBeforeError; i++) {
            ResultMessage message = queue.take();
            assert message.getStatus() == DownloadStatus.DOWNLOADING;
        }
        ResultMessage message = queue.take();
        assert message.getStatus() == DownloadStatus.ERROR;

    }

}