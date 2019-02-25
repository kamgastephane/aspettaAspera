package agoda.configuration;

public class DownloaderConfigurationImpl implements DownloaderConfiguration {


    private long segmentMinSize = 10 * 1024 * 1024; //10Mb

    private long segmentMaxSize = 5 * 100 * 1024 * 1024; //500Mb

    private int maxConcurrency = 10;

    private int maxRetry = 3;

    @Override
    public long getSegmentMinSize() {
        return segmentMinSize;
    }

    public void setSegmentMinSize(long segmentMinSize) {
        this.segmentMinSize = segmentMinSize;
    }

    @Override
    public long getSegmentMaxSize() {
        return segmentMaxSize;
    }

    public void setSegmentMaxSize(long segmentMaxSize) {
        this.segmentMaxSize = segmentMaxSize;
    }

    @Override
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public int getMaxRetry() {
        return maxRetry;
    }
}
