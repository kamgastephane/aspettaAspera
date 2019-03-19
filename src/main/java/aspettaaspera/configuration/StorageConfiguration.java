package aspettaaspera.configuration;

public interface StorageConfiguration {

    /**
     * size of the buffer used while writing file using  the buffered outputStream in Kilobytes, should be valid integer value
     */
    int getOutputStreamBufferSize();

//    /**
//     * the storage mode we intends to use:
//     * 1 => BufferedFileStorage
//     * 2 => RandomAccessFileStorage
//     */
//    int getStorageMode();

    /**
     * @return the path to the folder to be used for the downloaded files
     */
    String getDownloadFolder();
}
