package aspettaaspera.storage;

public class StorageFactoryImpl implements StorageFactory {

    private StorageFactoryImpl() {
    }

    public static StorageFactoryImpl getInstance() {
        return StorageFactoryImpl.StorageFactoryImplSingleton.INSTANCE;
    }
    private static class StorageFactoryImplSingleton {
        private static final StorageFactoryImpl INSTANCE = new StorageFactoryImpl();
    }

//    public static Storage getStorage(String url, String destination) {
//        Storage storage = null;
//        try {
//            storage = new RandomAccessStorage(url, destination);
//
//        } catch (IOException e) {
//            logger.error("IO exception while creating a storage related to " + url, e);
//        } catch (IllegalArgumentException e) {
//            logger.error("IllegalArgumentException exception while creating a storage related to " + url, e);
//        }
//        return storage;
//    }
//public static LazyRandomAccessStorage getStorage(String url, String destination, int bufferSize) {
//
//}
    public StorageSupplier getStorage(String url, String destination, int bufferSize) {
        return new LazyBufferedStorage(url,destination,bufferSize);
    }


}
