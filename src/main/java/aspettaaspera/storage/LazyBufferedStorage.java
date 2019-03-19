package aspettaaspera.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Supplier for the storage class,
 * Allow us to delay the creation of the bufferedStorage until it is ready to be used
 */
public class LazyBufferedStorage implements StorageSupplier {

    private Storage storage;
    private String srcUrl;
    private String downloadFolder;
    private int bufferSize;

    private Supplier<Storage> supplier;
    private boolean attemptInit;

    private static final Logger logger = LogManager.getLogger();

    public LazyBufferedStorage(String srcUrl, String downloadFolder, int bufferSize) {
        this.srcUrl = srcUrl;
        this.downloadFolder = downloadFolder;
        this.bufferSize = bufferSize;
        supplier = this::init;
    }

    public LazyBufferedStorage(Supplier<Storage> supplier) {
        this.supplier = supplier;
    }

    private Storage init()
    {
         if(!attemptInit){
             try {
                 storage = new BufferedStorage(srcUrl, downloadFolder, bufferSize);
             } catch (IOException e) {
                 logger.error("IO exception while creating a storage related at " + downloadFolder, e);
             } catch (IllegalArgumentException e) {
                 logger.error("IllegalArgumentException exception while creating a storage related at " + downloadFolder, e);
             }
         }
        return storage;
    }
    public boolean isInit()
    {
        return storage!=null;
    }
    @Override
    public Storage get() {
        Storage storage = supplier.get();
        attemptInit = true;
        return storage;
    }
}
