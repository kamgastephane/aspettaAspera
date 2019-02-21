package agoda.configuration;

public class ConfigurationImpl implements Configuration{

    private DownloaderConfiguration downloaderConfiguration;

    private StorageConfiguration storageConfiguration;


    @Override
    public DownloaderConfiguration getDownloaderConfiguration() {
        return downloaderConfiguration;
    }

    @Override
    public StorageConfiguration getStorageConfiguration() {
        return storageConfiguration;
    }


}
