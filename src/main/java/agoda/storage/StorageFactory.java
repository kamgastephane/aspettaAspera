package agoda.storage;

public interface StorageFactory {
    StorageSupplier getStorage(String url, String destination, int bufferSize);

    }
