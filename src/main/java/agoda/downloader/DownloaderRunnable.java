package agoda.downloader;


import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ChunkConsumer;
import agoda.protocols.ProtocolHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;


public class DownloaderRunnable implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    private ProtocolHandler handler;
    private LinkedBlockingQueue<ResultMessage> queue;
    private DownloadBlock block;
    DownloaderRunnable(Segment segment, ProtocolHandler protocolHandler, LinkedBlockingQueue<ResultMessage> resultMessageBlockingQueue) {


        this.handler = protocolHandler;
        block = new DownloadBlock(segment);
        queue = resultMessageBlockingQueue;


    }
    private ChunkConsumer consumer = new ChunkConsumer() {
        @Override
        public boolean consume(byte[]bytes) throws DownloadException {
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
                            block.getStatus(), bytes);
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


    @Override
    public void run() {

        // i should define which download method should be used
        // if the chunk size is 0 we use the direct download otherwise we use the Range based download
        boolean useRangeRequest = false;
        if(block.getRequestRange()>0)
        {
            useRangeRequest = true;
        }
        block.setStatus(DownloadStatus.DOWNLOADING);

        while (!Thread.currentThread().isInterrupted() && block.getStatus() == DownloadStatus.DOWNLOADING) {
            long start = block.getStartPosition();
            long end = start + block.getRequestRange() -1;

            try {
                long watchStart = System.currentTimeMillis();


                if(useRangeRequest)
                {
                    handler.download(block.getSrcUrl(),start,end, consumer);
                }else {
                    handler.download(block.getSrcUrl(), consumer);
                }
                long watchEnd = System.currentTimeMillis();
                block.updateRate(watchEnd - watchStart);

            } catch (DownloadException e) {
                logger.error(e.getMessage(), e.getCause());
                block.setLastError(e);
                Throwable ancestor = e;
                boolean foundInterruptedException = false;
                while ( ancestor.getCause() != null && e.getCause() != e)
                {

                    ancestor = ancestor.getCause();
                    if(ancestor instanceof InterruptedException)
                    {
                        logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
                        foundInterruptedException = true;
                        break;
                    }
                }
                if (foundInterruptedException){
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
                block.getStatus());

        try {
            queue.put(resultMessage);
        } catch (InterruptedException e) {
            logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
        }


    }


}
