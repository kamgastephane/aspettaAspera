package agoda.downloader;


import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ProtocolHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;


public class DownloaderRunnable implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    private static ThreadLocal<Segment> segmentThreadLocal = new ThreadLocal<>();
    ProtocolHandler handler;
    LinkedBlockingQueue<ResultMessage> queue;

    public DownloaderRunnable(Segment segment,ProtocolHandler protocolHandler, LinkedBlockingQueue<ResultMessage> resultMessageBlockingQueue)
    {

        this.handler = protocolHandler;
        segmentThreadLocal.set(segment);
        this.queue = resultMessageBlockingQueue;
    }

    @Override
    public void run() {

        segmentThreadLocal.get().setStatus(DownloadStatus.DOWNLOADING);

        while (!Thread.interrupted() && segmentThreadLocal.get().getStatus() == DownloadStatus.DOWNLOADING)
        {
            long start = segmentThreadLocal.get().getStartPosition();
            if (segmentThreadLocal.get().getStartPosition() == 0)
            {
                //we are just starting with this segment
                logger.info("start handling segment {} with range [{} - {}]",segmentThreadLocal.get().getSegmentIndex()
                        ,segmentThreadLocal.get().getInitialStartPosition(),segmentThreadLocal.get().getEndPosition());

                start = segmentThreadLocal.get().getInitialStartPosition();
            }

            try {
                long watchStart = System.currentTimeMillis();
                byte[] data = handler.download(segmentThreadLocal.get().getSrcUrl(), start);
                long watchEnd = System.currentTimeMillis();

                ResultMessage resultMessage = new ResultMessage(segmentThreadLocal.get().getSegmentIndex(),
                        segmentThreadLocal.get().getStatus(),data);
                queue.put(resultMessage);

                long byteRead = data.length;
                segmentThreadLocal.get().update(byteRead,watchEnd - watchStart);


            } catch (DownloadException e) {
                logger.error(e.getMessage(),e.getCause());
                segmentThreadLocal.get().setLastError(e);

                if(!segmentThreadLocal.get().canRetry())
                {
                    segmentThreadLocal.get().setStatus(DownloadStatus.ERROR);
                    break;
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted exception while pushing message to manager with status "+segmentThreadLocal.get().getStatus().toString(),e);
                break;
            }

        }
        //when i get here i am either interrupted, in an error state or finished state

        ResultMessage resultMessage = new ResultMessage(segmentThreadLocal.get().getSegmentIndex(),
                segmentThreadLocal.get().getStatus());

        try {
            queue.put(resultMessage);
        } catch (InterruptedException e) {
            logger.error("Interrupted exception while pushing message to manager with status "+segmentThreadLocal.get().getStatus().toString(),e);
        }


    }



}
