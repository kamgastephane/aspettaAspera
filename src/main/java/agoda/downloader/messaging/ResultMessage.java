package agoda.downloader.messaging;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadStatus;

public class ResultMessage implements DownloaderMessage {

    private int segmentId;

    private DownloadStatus status;

    private DownloadException downloadException;

    private byte[] content;


    public ResultMessage(int segmentId, DownloadStatus status) {
        this.segmentId = segmentId;
        this.status = status;
    }

    public ResultMessage(int segmentId, DownloadStatus status, byte[] content) {
        this.segmentId = segmentId;
        this.status = status;
        this.content = content;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setDownloadException(DownloadException downloadException) {
        this.downloadException = downloadException;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public int getSegmentId() {
        return segmentId;
    }

    @Override
    public Type getType() {
        return Type.RESULT;
    }
}
