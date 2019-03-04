package agoda.storage;

public interface StorageSupplier {
    public boolean isInit();
    public Storage get();
}
