package agoda.configuration;

public interface StorageConfiguration {

    /**
     * size of the buffer used while writing file using  the buffered outputStream in Kilobytes
     */
    public long getOutputStreamBufferSize();
}
