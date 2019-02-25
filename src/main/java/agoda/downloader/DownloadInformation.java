package agoda.downloader;

/**
 * This class contains  basics details about the stream we are trying to download
 */
public class DownloadInformation {

    private String srcUrl;

    private boolean acceptRange = false;

    private long size = 0;

    DownloadInformation(String srcUrl, boolean acceptRange, long size) {
        this.srcUrl = srcUrl;
        this.acceptRange = acceptRange;
        this.size = size;
    }

    public DownloadInformation(String srcUrl) {
        this.srcUrl = srcUrl;
    }

    public boolean isAcceptRange() {
        return acceptRange;
    }

    public long getSize() {
        return size;
    }

    String getSrcUrl() {
        return srcUrl;
    }
}
