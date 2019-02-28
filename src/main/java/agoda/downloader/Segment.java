package agoda.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * Note: this class has a natural ordering that is inconsistent with equals
 */
public class Segment implements Comparable<Segment> {

    private static final Logger logger = LogManager.getLogger();

    protected int segmentIndex;
    protected String srcUrl;
    protected long initialStartPosition;
    protected long endPosition;
    protected DownloadStatus status;
    protected long startPosition;
    protected int currentTry = 1;
    protected int maxRetry;
    protected int requestRange;


    protected Segment(int segmentIndex, String srcUrl, long initialStartPosition, long endPosition, int maxRetry, int requestRange) {
        this.segmentIndex = segmentIndex;
        this.srcUrl = srcUrl;
        this.initialStartPosition = initialStartPosition;
        this.endPosition = endPosition;
        this.maxRetry = maxRetry;
        this.requestRange = requestRange;
    }

    DownloadStatus getStatus() {
        return status;
    }

    void setStatus(DownloadStatus status) {
        this.status = status;
    }

    boolean isDownloading() {
        return DownloadStatus.DOWNLOADING.equals(status);
    }

    boolean isFinished() {
        return DownloadStatus.FINISHED.equals(status);
    }

    void downloading() {
        status = DownloadStatus.DOWNLOADING;
    }

    boolean isIdle() {
        return DownloadStatus.IDLE.equals(status);
    }



    int getSegmentIndex() {
        return segmentIndex;
    }

    long getStartPosition() {
        return startPosition;
    }

    public long getRequestRange() {
        return requestRange;
    }

    long getInitialStartPosition() {
        return initialStartPosition;
    }

    long getEndPosition() {
        return endPosition;
    }


    String getSrcUrl() {
        return srcUrl;
    }

    protected void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    @Override
    public int compareTo(Segment o) {
        return this.getSegmentIndex() - o.getSegmentIndex();
    }

    public static class SegmentBuilder {
        private int segmentIndex;
        private String srcUrl;
        private long initialStartPosition = 0;
        private long endPosition = 0;
        private int maxRetry;
        private int requestRange;

        SegmentBuilder setSegmentIndex(int segmentIndex) {
            this.segmentIndex = segmentIndex;
            return this;
        }

        SegmentBuilder setSrcUrl(String srcUrl) {
            this.srcUrl = srcUrl;
            return this;
        }

        SegmentBuilder setInitialStartPosition(long initialStartPosition) {
            this.initialStartPosition = initialStartPosition;
            return this;
        }

        SegmentBuilder setEndPosition(long endPosition) {
            this.endPosition = endPosition;
            return this;
        }

        SegmentBuilder setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        SegmentBuilder setRequestRange(int requestRange) {
            this.requestRange = requestRange;
            return this;
        }

        Segment createSegment() {
            return new Segment(segmentIndex, srcUrl, initialStartPosition, endPosition, maxRetry, requestRange);
        }
    }
}
