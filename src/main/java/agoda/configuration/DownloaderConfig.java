package agoda.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloaderConfig implements DownloaderConfiguration {

    @JsonProperty("minsize")
    private long segmentMinSize = 10 * 1024 * 1024; //10Mb
    @JsonProperty("maxsize")
    private long segmentMaxSize = 5 * 100 * 1024 * 1024; //500Mb
    @JsonProperty("maxconcurrency")
    private int maxConcurrency = 10;
    @JsonProperty("maxretry")
    private int maxRetry = 3;
    @JsonProperty("chunksize")
    private int chunkSize = 1024 * 1024;  //1Mb


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

    @Override
    public int getChunkSize() {
        return chunkSize;
    }


    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
