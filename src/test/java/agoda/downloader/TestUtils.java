package agoda.downloader;

import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.storage.*;

import java.io.File;

public class TestUtils {




    static StorageFactory getMockStorageFactory(long size)
    {
        return new StorageFactory() {
            @Override
            public StorageSupplier getStorage(String url, String destination, int bufferSize) {
                return new StorageSupplier() {
                    boolean isInit;
                    MockStorage storage;
                    @Override
                    public boolean isInit() {
                        return isInit;
                    }

                    @Override
                    public Storage get() {
                        if (!isInit)
                        {
                            isInit = true;
                            storage = new MockStorage((int) size);
                        }
                        return storage;
                    }
                };
            }
        };
    }

    public static DownloaderConfiguration getMockDownloaderConfiguration(long minSize, long maxSize, int maxConcurrency, int maxRetry, int chunsize,boolean useAdaptiveSegmentScheduler)
    {
        return new DownloaderConfiguration() {
            @Override
            public long getSegmentMinSize() {
                return minSize;
            }

            @Override
            public long getSegmentMaxSize() {
                return maxSize;
            }

            @Override
            public int getMaxConcurrency() {
                return maxConcurrency;
            }

            @Override
            public int getMaxRetry() {
                return maxRetry;
            }

            @Override
            public int getChunkSize() {
                return chunsize;
            }

            @Override
            public boolean useAdaptiveScheduler() {
                return useAdaptiveSegmentScheduler;
            }
        };
    }
    static LazyBufferedStorage getMockLazyStorage(Storage storage)
    {
        return new LazyBufferedStorage(() -> storage);
    }
    public static StorageConfiguration getMockStorageConfiguration(long bufferSize, File directory)
    {
        return new StorageConfiguration() {
            @Override
            public int getOutputStreamBufferSize() {
                return (int)bufferSize;
            }



            @Override
            public String getDownloadFolder() {

                return directory.getAbsolutePath();

            }
        };
    }
    static StorageConfiguration getMock(long bufferSize,String directory)
    {
        return new StorageConfiguration() {
            @Override
            public int getOutputStreamBufferSize() {
                return (int)bufferSize;
            }



            @Override
            public String getDownloadFolder() {

                return directory;

            }
        };
    }


}
