package agoda.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StorageConfig implements StorageConfiguration {

    @JsonProperty("buffersize")
    private int outputStreamBufferSize = 2 * 1024 * 1024;

//    private int storageMode = 1;
    @JsonProperty("folder")
    private String downloadFolder = null;


    @Override
    public int getOutputStreamBufferSize() {
        return outputStreamBufferSize;
    }


    public void setOutputStreamBufferSize(int outputStreamBufferSize) {
        this.outputStreamBufferSize = outputStreamBufferSize;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    @Override
    public String getDownloadFolder() {
        return downloadFolder;
    }
}
