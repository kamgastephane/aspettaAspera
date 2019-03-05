package agoda.downloader;


import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ProgressListener;
import agoda.protocols.ProtocolHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;


public class DownloaderRunnable implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    private ProtocolHandler handler;
    private LinkedBlockingQueue<ResultMessage> queue;
    private DownloadBlock block;
    private int writeCount = 0;
    private ProgressListener consumer = new ProgressListener() {
        @Override
        public boolean consume(byte[]bytes) throws DownloadException {

            writeCount++;
            if(bytes == null)
            {
                block.update(0);
                return false;
            }else{
                long byteRead = bytes.length;
                if(byteRead == 0)
                {
                    //empty array signals a requested end of the transmission by the protocol
                    block.setStatus(DownloadStatus.FINISHED);
                    return false;
                }else {
                    //we truncate the data received if we are out of bound => we are bigger than the size of the segment
                    boolean outOfBounds = block.isRangeOutOfBounds(byteRead);
                    if(outOfBounds)
                    {
                        int byteRequired = (int)(block.getEndPosition()-block.getStartPosition())+1;

                        byte[]copy = new byte[byteRequired];
                        System.arraycopy(bytes,0,copy,0,byteRequired);
                        byteRead = byteRequired;
                        bytes = copy;
                    }
                    block.update(byteRead);

                    ResultMessage resultMessage = new ResultMessage(block.getSegmentIndex(),
                            block.getStatus(), bytes,block.getRate());
                    try {
                        queue.put(resultMessage);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new DownloadException("InterruptedException while consuming package",e);
                    }
                    return true;

                }
            }
        };
    };
    //TODO use correlationID in the logs
    DownloaderRunnable(Segment segment, ProtocolHandler protocolHandler, LinkedBlockingQueue<ResultMessage> resultMessageBlockingQueue) {


        this.handler = protocolHandler;
        block = new DownloadBlock(segment);
        queue = resultMessageBlockingQueue;


    }

    @Override
    public void run() {

        // i should define which download method should be used
        // if the chunk size is 0 we use the direct download otherwise we use the Range based download
        boolean useRangeRequest = false;
        if (block.getRequestRange() > 0) {
            useRangeRequest = true;
        }
        block.setStatus(DownloadStatus.DOWNLOADING);

        while (!Thread.currentThread().isInterrupted() && block.getStatus() == DownloadStatus.DOWNLOADING) {
            long start = block.getStartPosition();
            long end = start + block.getRequestRange() - 1;

            try {

                //the writeCount is used to make sure at each cycle, the Consume method is getting called, A countDownLatch could be used as well for a thread safe
                writeCount = 0;

                Instant now = Instant.now();
                if (useRangeRequest) {
                    handler.download(block.getSrcUrl(), start, end, consumer);
                } else {
                    handler.download(block.getSrcUrl(), consumer);
                }

                Instant then = Instant.now();
                Duration duration = Duration.between(now, then);

                if (writeCount == 0) {
                    //nothing was written to the disk, something maybe went wrong, i handle it as an error
                    block.update(0);
                }

                block.updateRate(duration.toMillis());

            } catch (DownloadException e) {
                logger.error(e.getMessage(), e.getCause());
                block.setLastError(e);
                Throwable ancestor = e;
                boolean foundInterruptedException = false;
                while (ancestor.getCause() != null && e.getCause() != e) {

                    ancestor = ancestor.getCause();
                    if (ancestor instanceof InterruptedException) {
                        logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
                        foundInterruptedException = true;
                        break;
                    }
                }
                if (foundInterruptedException) {
                    block.setStatus(DownloadStatus.ERROR);
                    break;
                }

                if (!block.canRetry()) {
                    block.setStatus(DownloadStatus.ERROR);
                    break;
                }
            }

        }
        //when i get here i am either interrupted, in an error state or finished state

        ResultMessage resultMessage = new ResultMessage(block.getSegmentIndex(),
                block.getStatus(), block.getRate());

        try {
            queue.put(resultMessage);
        } catch (InterruptedException e) {
            logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
        }


    }

}


