package agoda.downloader;

import agoda.configuration.DownloaderConfiguration;

import java.util.List;

public interface SegmentsCalculator {


    /**
     * this method should divide the stream to download in segment which can then be downloaded separately
     *
     * @param desiredSegmentCount the number of segment we would like to have, which is equivalent to the number of concurrent stream downloaded
     * @param configuration       the configuration of the downloader
     * @param information         informations about the stream to be downloaded
     * @return a division of the stream in segment with a defined size
     */
    List<Segment> getSegments(int desiredSegmentCount, DownloaderConfiguration configuration, DownloadInformation information);

}
