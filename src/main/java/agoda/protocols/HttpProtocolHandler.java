package agoda.protocols;

import agoda.downloader.DownloadInformation;

public class HttpProtocolHandler implements ProtocolHandler {

    public static final String HTTP_SCHEME = "http";

    @Override
    public String getScheme() {
        return HTTP_SCHEME;
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
