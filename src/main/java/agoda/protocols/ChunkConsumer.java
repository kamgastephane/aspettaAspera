package agoda.protocols;

/**
 * relay between the {@link ProtocolHandler} and the {@link agoda.downloader.DownloaderRunnable}
 * Every time a block of bytes is download from by the protocol the consume method can be called to pass that byte chunk to the relevant layer AKA the disk
 */
public interface ChunkConsumer {

    /**
     * @param bytes pass a chunk of bytes to the {@link agoda.downloader.DownloaderRunnable} to be save on the disk
     * @return {@code false} if the {@link ProtocolHandler} should stop downloading
     * @throws InterruptedException
     */
    boolean consume(byte[]bytes) throws InterruptedException;
}