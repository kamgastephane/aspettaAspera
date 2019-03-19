package aspettaaspera.protocols;

import aspettaaspera.downloader.DownloadException;
import aspettaaspera.downloader.ResourceInformation;

import java.util.Map;


public interface ProtocolHandler {

    byte[] EOF = {0};


    String getScheme();

    /**
     * @param url  the url we want to download
     * @param from the start of the segment we want to download
     * @param to   the end of the segment we want to download
     * @param consumer   the {@link ProgressListener} we pass the byte download to
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     *  in case of any {@link InterruptedException} or similar code which can erase the interrupt flag, if you decide to handle it, remember to set the flag back to avoid an infinite loop
     */
    void download(String url, long from, long to, ProgressListener consumer) throws DownloadException;

    /**
     * @param url  the url we want to download
     * @param consumer   the {@link ProgressListener} we pass the byte download to
     * @throws DownloadException in case of logical error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     *  in case of any {@link InterruptedException} or similar code which can erase the interrupt flag, if you decide to handle it, remember to set the flag back to avoid an infinite loop
     */
    void download(String url, ProgressListener consumer) throws DownloadException;


    /**
     * @param url        the url we want to download
     * @param parameters list of parameters related to the download, and specific to the protocol e.g. username,password, proxy, key...
     * @param consumer   the {@link ProgressListener} we pass the byte download to
     * @throws DownloadException in case of logical error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     *                           in case of any {@link InterruptedException} or similar code which can erase the interrupt flag, if you decide to handle it, remember to set the flag back to avoid an infinite loop
     */
    default void download(String url, Map<String, String> parameters, ProgressListener consumer) throws DownloadException {

    }


    /**
     * @param url the url we want some information about
     * @param parameters list of parameters related to the url, and specific to the protocol e.g. username,password, proxy, key...
     *            if {@link ResourceInformation#isAcceptRange()} is set at true, the download will be done on multiple thread
     * @return the filled {@link ResourceInformation}
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     *                           this allow the retry system to kick in
     */
    ResourceInformation getInfo(String url, Map<String,String> parameters) throws DownloadException;


    /**
     * //TODO remove the default
     * clean any resource used by this protocolHandler
     */
    default void close() {

    }
}
