package agoda.downloader.messaging;

import agoda.downloader.DownloadStatus;

public class ResultMessage implements DownloaderMessage {

    private int segmentId;

    private DownloadStatus status;

    private double rate;

    private byte[] content;



    public ResultMessage(int segmentId, DownloadStatus status) {
       this(segmentId,status,0);
    }
    public ResultMessage(int segmentId, DownloadStatus status,double rate) {
        this(segmentId,status,null,0);

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
