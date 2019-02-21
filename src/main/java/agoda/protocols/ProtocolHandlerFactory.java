package agoda.protocols;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *  this small factory will return the appropriate handler based on the protocol.
 *  To handle a new protocol, we should add it the following switch...case block
 */
public class ProtocolHandlerFactory {

    private static final Logger logger = LogManager.getLogger();

    public static ProtocolHandler get(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            logger.error("Received an malformed uri: "+url,e);
            return null;
        }
        String scheme = uri.getScheme();
        switch (scheme) {
            case HttpProtocolHandler.HTTP_SCHEME:
                return new HttpProtocolHandler();
            case FTPProtocolHandler.FTP_SCHEME:
                return new FTPProtocolHandler();
            default:
                return null;
        }
    }
}
