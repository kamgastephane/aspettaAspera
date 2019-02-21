package agoda.downloader.messaging;

public interface DownloaderMessage {

    public enum Type
    {
        /**
         * message to notify the the segment download should be aborted
         */
        POISONPILL,
        /**
         * message to send even partial/complete result
         */
        RESULT
    }

    /**
     * @return  the ID of the segment this message is related to
     */
    public int getSegmentId();

    public Type getType();


}
