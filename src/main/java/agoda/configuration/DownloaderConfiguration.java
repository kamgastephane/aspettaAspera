package agoda.configuration;

public interface DownloaderConfiguration {
    /**
     * min size of a file segment to be downloaded  by a single thread
     */
    long getSegmentMinSize();

    /**
     * max size of a file segment to be downloaded  by a single thread
     */
    long getSegmentMaxSize();

    /**
     * max concurrency allowed between all downloads
     */
    int getMaxConcurrency();

    /**
     * max retry tentative for a single segment
     */
    int getMaxRetry();


}
