package agoda.protocols;

import agoda.downloader.DownloadException;

/**
 * relay between the {@link ProtocolHandler} and the {@link agoda.downloader.DownloaderRunnable}
 * Every time a block of bytes is downloaded by the protocol, the consume method can be called to pass that byte chunk to the relevant layer AKA the disk
 */
public interface ProgressListener {

    /**
     * @param buffer pass a chunk of bytes to the {@link agoda.downloader.DownloaderRunnable} to be save on the disk
     * @return {@code false} if the {@link ProtocolHandler} should stop downloading, {@code true} otherwise
     * @throws DownloadException if an error happened while consuming the bytes
     */
    boolean consume(byte[]buffer) throws DownloadException;
}
