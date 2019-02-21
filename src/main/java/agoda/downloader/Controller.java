package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.protocols.ProtocolHandler;
import agoda.protocols.ProtocolHandlerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller {

    private int maxConcurrentDownload;
    private String url;
    private Configuration configuration;
    private static final Logger logger = LogManager.getLogger();


    public Controller( Configuration configuration, int maxConcurrentDownload, String url) {
        this.maxConcurrentDownload = maxConcurrentDownload;
        this.url = url;

        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        if(protocolHandler == null)
        {
            //TODO test this
            logger.error("Received url {} but no protocol handler defined for it", url);
            throw new IllegalArgumentException("no protocol handler found for url" + url);
        }
    }

    public void setMaxConcurrentDownload(int maxConcurrentDownload) {
        this.maxConcurrentDownload = maxConcurrentDownload;
    }
}
