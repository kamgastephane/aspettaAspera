package agoda.downloader.messaging;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadStatus;

public class ResultMessage implements DownloaderMessage {

    private int segmentId;

    private DownloadStatus status;

    private double rate;

    private byte[] content;



    public ResultMessage(int segmentId, DownloadStatus status) {
        this.segmentId = segmentId;
        this.status = status;
    }
    public ResultMessage(int segmentId, DownloadStatus status,double rate) {
        this.segmentId = segmentId;
        this.status = status;
        this.rate = rate;
    }
    public ResultMessage(int segmentId, DownloadStatus status, byte[] content,double rate) {
        this.segmentId = segmentId;
        this.status = status;
        this.content = content;
        this.rate = rate;
    }

    public double getRate() {
        return rate;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setRate(long rate) {
        this.rate = rate;
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
