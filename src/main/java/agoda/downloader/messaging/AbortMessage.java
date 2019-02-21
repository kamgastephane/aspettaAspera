package agoda.downloader.messaging;

public class AbortMessage implements DownloaderMessage {

    private int segmentId;

    public AbortMessage(int segmentId) {
        this.segmentId = segmentId;
    }

    @Override
    public int getSegmentId() {
        return segmentId;
    }

    @Override
    public Type getType() {
        return Type.POISONPILL;
    }
}
