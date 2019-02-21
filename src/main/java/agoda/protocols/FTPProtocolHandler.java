package agoda.protocols;

import agoda.downloader.DownloadInformation;

public class FTPProtocolHandler implements ProtocolHandler {

    public static final String FTP_SCHEME = "FTP";

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public byte[] download(String url, long from, long to) {
        return new byte[0];
    }

    @Override
    public byte[] download(String url, long from) {
        return new byte[0];
    }

    @Override
    public DownloadInformation getInfo(String url) {
        return null;
    }
}
