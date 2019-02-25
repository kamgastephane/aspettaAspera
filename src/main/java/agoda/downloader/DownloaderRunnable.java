package agoda.downloader;


import agoda.downloader.messaging.ResultMessage;
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

    @Override
    public void run() {

        //we use thread local as a hack to provide synchronization
        //we can guarantee that the copy hold by the controller is not affecting this one

        block.setStatus(DownloadStatus.DOWNLOADING);

        while (!Thread.currentThread().isInterrupted() && block.getStatus() == DownloadStatus.DOWNLOADING) {
            long start = block.getStartPosition();


            try {
                long watchStart = System.currentTimeMillis();
                byte[] data = handler.download(block.getSrcUrl(), start);
                long watchEnd = System.currentTimeMillis();

                if(data == null)
                {
                    //null signals a requested end of the transmission by the protocol
                    block.setStatus(DownloadStatus.FINISHED);
                    break;
                }
                else{
                    long byteRead = data.length;


                    //we truncate the data received if we are out of bound => we are bigger than the size of the segment
                    boolean outOfBounds = block.isRangeOutOfBounds(byteRead);
                    if(outOfBounds)
                    {
                        int byteRequired = (int)(block.getEndPosition()-start)+1;

                        byte[]copy = new byte[byteRequired];
                        System.arraycopy(data,0,copy,0,byteRequired);
                        byteRead = byteRequired;
                        data = copy;
                    }
                    block.update(byteRead, watchEnd - watchStart);

                    if (byteRead > 0) {
                        ResultMessage resultMessage = new ResultMessage(block.getSegmentIndex(),
                                block.getStatus(), data);
                        queue.put(resultMessage);

                    }
                }





            } catch (DownloadException e) {
                logger.error(e.getMessage(), e.getCause());
                block.setLastError(e);
                if(e.getCause() instanceof InterruptedException)
                {
                    logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
                    break;

                }
                if (!block.canRetry()) {
                    block.setStatus(DownloadStatus.ERROR);
                    break;
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted exception while pushing message to manager with status " + block.getStatus().toString(), e);
                break;
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
