package aspettaaspera.configuration;

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
    /**
     * the size in bytes of a byte chunk download by a single http request while using Range requests
     * when the range request is not provided by the server, this parameter represents the size of one data block red from the http response
     * from the socket. By tweaking this parameter, one could have a increase the experience in case of slow clients
     */
    int getChunkSize();

    default boolean useAdaptiveScheduler() {
        return false;
    }


}
