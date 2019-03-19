package aspettaaspera.downloader;

public class DownloadException extends Exception {

    private String url;
    private long segmentStart;
    private long segmentSize;

    public DownloadException(String url, long segmentStart, long segmentSize, Throwable cause) {
        super(String.format("Exception %s occurred while downloading stream %s starting from segment %d of size %d"
                , cause.getClass().getName()
                , url
                , segmentStart
                , segmentSize)
                , cause);
        this.segmentSize = segmentSize;
        this.segmentStart = segmentStart;
        this.url = url;
    }

    public DownloadException(String url, Throwable cause) {
        super(String.format("Exception %s occurred while downloading stream %s "
                , cause.getClass().getName()
                , url)
                , cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSegmentStart() {
        return segmentStart;
    }

    public void setSegmentStart(long segmentStart) {
        this.segmentStart = segmentStart;
    }

    public long getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
    }
}
