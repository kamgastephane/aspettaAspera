package agoda.downloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Date;

public class DownloadBlock extends Segment {

    private static final Logger logger = LogManager.getLogger();
    private LocalDateTime lastReception;
    private DownloadException lastError;
    private double rate;// rate in kbit/s
    private long lastRangeRead = 0;
    DownloadBlock(Segment segment) {
        super(segment.segmentIndex,segment.srcUrl, segment.initialStartPosition, segment.endPosition, segment.maxRetry, segment.requestRange);
        this.startPosition = initialStartPosition;
    }

    boolean isRangeOutOfBounds(long range)
    {
        return (startPosition+range>endPosition && endPosition>0);
    }
    void updateRate(long durationInMs)
    {
        double sizeInKbit = (lastRangeRead * 8) / 1000D;
        double durationInSec = durationInMs * 1000D;
        rate = sizeInKbit / durationInSec;
        logger.info("Received {} kbit from server for url {} on segment {}, currentStatus:{} ", sizeInKbit, srcUrl, segmentIndex, status.toString());
        //TODO here i should add heuristics to handle low or fast connections at runtime

    }
    void update(long range) {
        lastRangeRead = range;
        if (range == 0) {
            //i did not receive any data something may be wrong so i consider it as an error
            logger.warn("Received empty response from server for url {} on segment {}", srcUrl, segmentIndex);

            boolean canRetry = canRetry();
            if (!canRetry) {
                //we set the status in error
                lastError = new DownloadException("Received too many empty response from server", new RuntimeException("Received too many empty response from server"));
                status = DownloadStatus.ERROR;
            }
        } else {

            //as we are always resetting the counter, a server could abuse us and send data 1 out of three time :D
            currentTry = 1;
            this.lastReception = LocalDateTime.now();
            this.startPosition += range;
            //if the end position is <=0 we have a download with unknow size
            if (startPosition > endPosition && endPosition>0) {
                this.status = DownloadStatus.FINISHED;
            }

        }

    }
    public void setLastReception(LocalDateTime lastReception) {
        this.lastReception = lastReception;
    }


    public void setRate(double rate) {
        this.rate = rate;
    }
    public DownloadException getLastError() {
        return lastError;
    }

    void setLastError(DownloadException lastError) {
        this.lastError = lastError;
    }


    boolean canRetry() {
        currentTry++;
        return currentTry <= maxRetry;
    }
}
