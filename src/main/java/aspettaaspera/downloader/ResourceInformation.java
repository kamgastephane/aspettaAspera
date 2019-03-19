package aspettaaspera.downloader;

/**
 * This class contains  basics details about the resource we are trying to download
 */
public class ResourceInformation {

    public static final long DEFAULT_SIZE = 0L;
    private String srcUrl;

    private boolean acceptRange = false;

    private long size = DEFAULT_SIZE;

    public ResourceInformation(String srcUrl, boolean acceptRange, long size) {
        this.srcUrl = srcUrl;
        this.acceptRange = acceptRange;
        this.size = size;
    }

    public ResourceInformation(String srcUrl) {
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
