package agoda.protocols;

import agoda.downloader.DownloadException;
import agoda.downloader.DownloadInformation;
import org.apache.commons.io.FileUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class HttpProtocolHandler implements ProtocolHandler {

    private static final Logger logger = LogManager.getLogger();

    private static final long MAX_CHUNK_SIZE = FileUtils.ONE_MB * 100;
    private static final int DEFAULT_CHUNK_SIZE = (int) (FileUtils.ONE_MB * 2);
    private static final int RETRY = 2;

    private DefaultHttpRequestRetryHandler retryhandler = new DefaultHttpRequestRetryHandler(RETRY, true);
    //TODO keep the connection alive, verify the multithread issues
    //TODO somebody must clease the client as well
    private final CloseableHttpClient httpclient = HttpClients.custom()
            .setRetryHandler(retryhandler).build();


    static final String HTTP_SCHEME = "http";


    @Override
    public String getScheme() {
        return HTTP_SCHEME;
    }

    @Override
    public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.RANGE, "bytes=" + (from) + "-" + (to));


            try (CloseableHttpResponse response = httpclient.execute(request)) {
                StatusLine statusLine = response.getStatusLine();

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    //we check if the server respected the byte range request
                    if (statusLine.getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
                        //i trusted that the server sent me the correct content length
                        byte[] bytes = EntityUtils.toByteArray(entity);
                        consumer.consume(bytes);
                    }else {
                        logger.error("Expected a Partial content entity at {}, but none was supplied Returning error to the DownloadRunnable", url);
                        consumer.consume(null);

                    }
                } else {
                    //supply a null to the consumer
                    logger.error("Received a null entity at {}, Returning an error to the DownloadRunnable", url);
                    consumer.consume(null);
                }
            }
        } catch (IOException  e) {
            throw new DownloadException("Exception when downloading " + url, e);
        }
    }

    @Override
    public void download(String url, ProgressListener consumer) throws DownloadException {
        try {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    stream(entity, DEFAULT_CHUNK_SIZE, consumer);
                    // here this download is finish i send a finish message
                    consumer.consume(EOF);

                } else {
                    logger.error("Received a null entity at {}, Returning an error to the DownloadRunnable", url);
                    consumer.consume(null);
                }
            }
        } catch (IOException  e) {
            throw new DownloadException("Exception when downloading " + url, e);
        }
    }

    private void stream(HttpEntity provider, int chunkSize, ProgressListener consumer) throws DownloadException, IOException {
        byte[] result = new byte[chunkSize];
        try (InputStream stream = provider.getContent()) {
            boolean shouldConsume = true;
            while (stream.read(result) != -1 && shouldConsume) {
                //supply data to the consumer
                shouldConsume = consumer.consume(result);
            }
        }
    }


    @Override
    public DownloadInformation getInfo(String url) throws DownloadException {
        try {
            HttpHead request = new HttpHead(url);
            request.setHeader(HttpHeaders.RANGE, "bytes=" + (0) + "-" + (1));

            try (CloseableHttpResponse response = httpclient.execute(request)) {

                HttpEntity entity = response.getEntity();

                long contentLength = entity.getContentLength();
                boolean acceptRange = false;
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
                    acceptRange = true;
                }
                return new DownloadInformation(url, acceptRange, contentLength);

            }
        }catch (IOException e) {
            throw new DownloadException("Exception when doing HEAD on " + url, e);
        }
    }
}
