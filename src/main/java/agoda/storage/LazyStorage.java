package agoda.storage;

import java.util.function.Supplier;

/**
 * Supplier for the storage class,
 * Allow us to delay the creation of the storage until it is ready to be used
 */
public class LazyStorage implements Supplier<Storage> {

    private Storage storage;
    private String srcUrl;
    private String downloadFolder;
    private int bufferSize;

    private Supplier<Storage> supplier;
    private boolean attemptInit;

    public LazyStorage(String srcUrl, String downloadFolder, int bufferSize) {
        this.srcUrl = srcUrl;
        this.downloadFolder = downloadFolder;
        this.bufferSize = bufferSize;
        supplier = this::init;
    }

    public LazyStorage(Supplier<Storage> supplier) {
        this.supplier = supplier;
    }

    private Storage init()
    {
         if(!attemptInit){
             storage  = StorageFactory.getStorage(srcUrl,downloadFolder,
                     bufferSize);
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
