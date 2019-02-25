package agoda.downloader;

import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.configuration.StorageConfigurationImpl;
import agoda.protocols.ProtocolHandler;
import agoda.storage.StorageFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TestUtils {



    public static List<Segment> getSegments(int count)
    {
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Segment s = new Segment.SegmentBuilder()
                    .setSegmentIndex(i)
                    .createSegment();
        }
        return segments;

    }
    public static File getTempDirectory() {
        try {
            return Files.createTempDirectory("agoda").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    static ProtocolHandler get(Supplier<byte[]> supplier)
    {
        return new ProtocolHandler() {
            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public byte[] download(String url, long from, long to) throws DownloadException {
                return download(url,from);
            }

            @Override
            public byte[] download(String url, long from) throws DownloadException {
                return supplier.get();
            }

            @Override
            public DownloadInformation getInfo(String url) throws DownloadException {
                return null;
            }
        };
    }
    static DownloaderConfiguration get(long minSize,long maxSize,int maxConcurrency,int maxRetry)
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
        };
    }
    static StorageConfiguration get(long bufferSize,File directory)
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



}
