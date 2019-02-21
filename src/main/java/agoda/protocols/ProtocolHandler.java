package agoda.protocols;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadInformation;


public interface ProtocolHandler {

    String getScheme();

    /**
     * @param url the url we want to download
     * @param from the start of the segment we want to download
     * @param to the end of the segment we want to download
     * @return a byte array which has been download
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     * this allow the retry system to kick in
     * */
    byte[] download(String url, long from, long to) throws DownloadException;

    /**
     * @param url the url we want to download
     * @param from the start of the segment we want to download
     * @return a byte array which has been download
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     * this allow the retry system to kick in
     * */
    byte[] download(String url, long from) throws DownloadException;


    /**
     * @param url the url we want some information about
     * The size should be always
     * if {@link DownloadInformation#isAcceptRange()} is set at true, the download will be done on multiple thread
     * @return the filled {@link DownloadInformation}
     * @throws DownloadException in case of error; in case of error of any kind, your code should wrap the error and throw a {@link DownloadException}
     * this allow the retry system to kick in
     */
    DownloadInformation getInfo(String url) throws DownloadException;
}