package agoda.downloader.messaging;

public interface DownloaderMessage {

    /**
     * @return the ID of the segment this message is related to
     */
    int getSegmentId();

    Type getType();

    enum Type {
        /**
         * message to notify the the segment download should be aborted
         */
        POISONPILL,
        /**
         * message to send even partial/complete result
         */
        RESULT
    }


}
