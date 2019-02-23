package agoda.configuration;

public class StorageConfigurationImpl implements StorageConfiguration {



    private int outputStreamBufferSize = 2 * 1024 * 1024;

    private String DownloadFolder = null;



    @Override
    public int getOutputStreamBufferSize() {
        return outputStreamBufferSize;
    }

    @Override
    public String getDownloadFolder() {
        return getDownloadFolder();
    }
}
