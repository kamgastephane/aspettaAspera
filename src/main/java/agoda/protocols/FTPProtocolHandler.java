package agoda.protocols;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadInformation;

public class FTPProtocolHandler implements ProtocolHandler {

    public static final String FTP_SCHEME = "FTP";


    @Override
    public String getScheme() {
        return FTP_SCHEME;
    }

    @Override
    public void download(String url, long from, long to, ChunkConsumer consumer) throws DownloadException {

    }

    @Override
    public void download(String url, ChunkConsumer consumer) throws DownloadException {

    }



    @Override
    public DownloadInformation getInfo(String url) {
        return null;
    }
}
