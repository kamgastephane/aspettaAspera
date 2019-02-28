package agoda.protocols;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadInformation;

import java.util.function.Consumer;


public interface ProtocolHandler {

    String getScheme();

    /**
     * @param url  the url we want to download
     * @param from the start of the segment we want to download
     * @param to   the end of the segment we want to download
     * @param consumer   the {@link ChunkConsumer} we pass the byte download to
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     *  in case of any {@link InterruptedException} or similar code which can erase the interrupt flag, if you decide to handle it, remember to set the flag back to avoid an infinite loop
     */
    void download(String url, long from, long to,ChunkConsumer consumer) throws DownloadException;

    /**
     * @param url  the url we want to download
     * @param consumer   the {@link ChunkConsumer} we pass the byte download to
     * @throws DownloadException in case of logical error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     *  in case of any {@link InterruptedException} or similar code which can erase the interrupt flag, if you decide to handle it, remember to set the flag back to avoid an infinite loop
     */
    void download(String url, ChunkConsumer consumer) throws DownloadException;


    /**
     * @param url the url we want some information about
     *            The size should be always
     *            if {@link DownloadInformation#isAcceptRange()} is set at true, the download will be done on multiple thread
     * @return the filled {@link DownloadInformation}
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     */
    DownloadInformation getInfo(String url) throws DownloadException;
}
