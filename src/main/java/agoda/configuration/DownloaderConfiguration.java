package agoda.configuration;

public interface DownloaderConfiguration {
    /**
     * min size of a file segment to be downloaded  by a single thread
     */
    public long getSegmentMinSize();
    /**
     * max size of a file segment to be downloaded  by a single thread
     */
    public long getSegmentMaxSize();
    /**
     * max concurrency allowed between all downloads
     */
    public int getMaxConcurrency();
    /**
     * max retry tentative for a single segment
     */
    public int getMaxRetry();


}
