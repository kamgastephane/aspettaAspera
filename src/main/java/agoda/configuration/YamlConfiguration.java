package agoda.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YamlConfiguration implements Configuration {

    @JsonProperty
    private DownloaderConfig downloader;

    @JsonProperty
    private StorageConfig storage;


    @Override
    public DownloaderConfiguration getDownloaderConfiguration() {
        return downloader;
    }

    @Override
    public StorageConfiguration getStorageConfiguration() {
        return storage;
    }

    public void setDownloader(DownloaderConfig downloader) {
        this.downloader = downloader;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    public YamlConfiguration() {
    }


}
