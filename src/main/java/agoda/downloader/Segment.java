package agoda.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;


public class Segment {

    private static final Logger logger = LogManager.getLogger();

    private long startPosition;
    private int segmentIndex;
    private String srcUrl;
    private long initialStartPosition;
    private long endPosition;
    private DownloadException lastError;
    private DownloadStatus status;
    private Date lastReception;

    private double rate;// rate in kbit/s
    private int currentTry = 1;
    private int maxRetry;


    private Segment(int segmentIndex, String srcUrl, long initialStartPosition, long endPosition, int maxRetry, long requestRange) {
        this.segmentIndex = segmentIndex;
        this.srcUrl = srcUrl;
        this.initialStartPosition = initialStartPosition;
        this.endPosition = endPosition;
        this.maxRetry = maxRetry;
    }

    public  void update(long range,long durationInMs)
    {
        if(range == 0)
        {
            //i did not receive any data something may be wron so i consider it as an error
            logger.warn("Received empty response from server for url {} on segment {}",srcUrl,segmentIndex);

            boolean canRetry = canRetry();
            if(!canRetry)
            {
                //we set the status in error
                lastError = new DownloadException("Received too many empty response from server",null);
                status = DownloadStatus.ERROR;
            }
        }
        else{

            //as we are always resetting the counter, a server could abuse us and send data 1 out of three time :D
            currentTry = 1;
            this.startPosition+=range;
            this.lastReception = new Date();
            if(startPosition>=endPosition)
            {
                this.status = DownloadStatus.FINISHED;
            }
            double sizeInKbit = (range * 8)/1000D;
            double durationInSec = durationInMs/1000D;
            rate = sizeInKbit/durationInSec;
            logger.info("Received {}kbit from server for url {} on segment {}, currentStatus:{} ",sizeInKbit,srcUrl,segmentIndex,status.toString());

        }

        //TODO here i should add heuristics to handle low or fast connections by tweaking
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public synchronized void setLastReception(Date lastReception) {
        this.lastReception = lastReception;
    }



    public synchronized void setRate(double rate) {
        this.rate = rate;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public long getInitialStartPosition() {
        return initialStartPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }

    public DownloadException getLastError() {
        return lastError;
    }

    public void setLastError(DownloadException lastError) {
        this.lastError = lastError;
    }

    public String getSrcUrl() {
        return srcUrl;
    }

    public boolean canRetry() {
        currentTry++;
        return currentTry <= maxRetry;
    }

    public static class SegmentBuilder {
        private int segmentIndex;
        private String srcUrl;
        private long initialStartPosition = 0;
        private long endPosition = 0;
        private int maxRetry;
        private long requestRange = 2 * FileUtils.ONE_MB;

        public SegmentBuilder setSegmentIndex(int segmentIndex) {
            this.segmentIndex = segmentIndex;
            return this;
        }

        public SegmentBuilder setSrcUrl(String srcUrl) {
            this.srcUrl = srcUrl;
            return this;
        }

        public SegmentBuilder setInitialStartPosition(long initialStartPosition) {
            this.initialStartPosition = initialStartPosition;
            return this;
        }

        public SegmentBuilder setEndPosition(long endPosition) {
            this.endPosition = endPosition;
            return this;
        }

        public SegmentBuilder setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        public void setRequestRange(long requestRange) {
            this.requestRange = requestRange;
        }

        public Segment createSegment() {
            return new Segment(segmentIndex, srcUrl, initialStartPosition, endPosition, maxRetry, requestRange);
        }
    }
}
