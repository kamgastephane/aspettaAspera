package aspettaaspera.downloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DownloadBlock extends Segment {

    private static final Logger logger = LogManager.getLogger();
    private LocalDateTime lastReception;
    private DownloadException lastError;
    private long lastRangeRead = 0;
    private List<Double> rates;

    DownloadBlock(Segment segment) {
        super(segment.segmentIndex,segment.srcUrl, segment.initialStartPosition, segment.endPosition, segment.maxRetry, segment.requestRange);
        this.startPosition = initialStartPosition;
        this.rates = new ArrayList<>();
    }

    boolean isRangeOutOfBounds(long range)
    {
        return (startPosition+range>endPosition && hasSize());
    }

    void updateRate(long durationInMs)
    {
        double sizeInKbyte = (lastRangeRead) / 1024D;
        double durationInSec = durationInMs / 1000D;
        rate = sizeInKbyte / durationInSec;
        rates.add(rate);
        logger.debug("Received {} kb from server for url {} on segment {}, currentStatus:{} ", sizeInKbyte, srcUrl, segmentIndex, status.toString());

    }

    public double   getRate() {
        if(rates.size() > 0)
        {
            double totalevent = rates.stream().mapToDouble(f -> f).sum();
            return totalevent / rates.size();
        }
        return 0;

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
            if (startPosition > endPosition && hasSize()) {
                this.status = DownloadStatus.FINISHED;
            }

        }

    }

    private boolean hasSize() {
        return endPosition>0;
    }

    public void setLastReception(LocalDateTime lastReception) {
        this.lastReception = lastReception;
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
