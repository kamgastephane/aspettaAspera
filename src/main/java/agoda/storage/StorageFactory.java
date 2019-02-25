package agoda.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class StorageFactory {

    private static final Logger logger = LogManager.getLogger();

    public static Storage getStorage(String url, String destination) {
        Storage storage = null;
        try {
            storage = new RandomAccessStorage(url, destination);

        } catch (IOException e) {
            logger.error("IO exception while creating a storage related to " + url, e);
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException exception while creating a storage related to " + url, e);
        }
        return storage;
    }

    public static Storage getStorage(String url, String destination, int buffersize) {
        Storage storage = null;
        try {
            storage = new BufferedStorage(url, destination, buffersize);

        } catch (IOException e) {
            logger.error("IO exception while creating a storage related to " + url, e);
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException exception while creating a storage related to " + url, e);
        }
        return storage;
    }

}
