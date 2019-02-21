package agoda.configuration;

public class StorageConfigurationImpl implements StorageConfiguration {



    private int outputStreamBufferSize = 2 * 1024 * 1024;



    @Override
    public long getOutputStreamBufferSize() {
        return outputStreamBufferSize;
    }
}
