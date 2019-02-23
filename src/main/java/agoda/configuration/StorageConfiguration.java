package agoda.configuration;

public interface StorageConfiguration {

    /**
     * size of the buffer used while writing file using  the buffered outputStream in Kilobytes, should be valid integer value
     */
    public int getOutputStreamBufferSize();

    public String getDownloadFolder();
}
