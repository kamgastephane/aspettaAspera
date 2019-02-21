package agoda.downloader;

import agoda.configuration.DownloaderConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BasicSegmentCalculatorTest {

    @Test
    public void getSegmentsWhenTheRangeIsSetAtOff() {
        BasicSegmentCalculator segmentCalculator = new BasicSegmentCalculator();

        DownloadInformation fileWithoutRangeHeader = get(false,2 * 1024 * 1024);
        DownloaderConfiguration configuration = get(2 * 1024 * 1024,2 * 1024 * 1024,100);
        List<Segment> segments = segmentCalculator.getSegments(configuration.getMaxConcurrency(), configuration, fileWithoutRangeHeader);

        assert (segments.size() ==1);


        assert (segments.get(0).getSegmentIndex()==0);
        assert (segments.get(0).getInitialStartPosition()==0);
        assert (segments.get(0).getStartPosition()==0);
        assert (segments.get(0).getEndPosition()==(2 * 1024 * 1024) - 1);
        assert (getSegmentSum(segments) == 2 * 1024 * 1024);

    }

    long getSegmentSum(List<Segment> list)
    {
        long total = 0;
        for (Segment segment : list)
        {
            total+= (segment.getEndPosition() - segment.getInitialStartPosition()+1);
        }
        return total;
    }

    @Test
    public void getSegmentsWhenTheDefaultSegmentSizeIsLessThanTheSegmentMinSize() {
        BasicSegmentCalculator segmentCalculator = new BasicSegmentCalculator();
        long size = 100 * 1024 * 1024;
        DownloadInformation file = get(true,size);
        DownloaderConfiguration configuration = get(2 * 1024 * 1024,500 * 1024 * 1024,100);
        List<Segment> segments = segmentCalculator.getSegments(configuration.getMaxConcurrency(), configuration, file);

        assertEquals(50, segments.size());


        assertEquals(49, segments.get(49).getSegmentIndex());
        assertEquals(segments.get(49).getInitialStartPosition(), 2 * 49 * 1024 * 1024);
        assertEquals(0, segments.get(49).getStartPosition());
        assertEquals(segments.get(49).getEndPosition(), (2 * 50 * 1024 * 1024) - 1);
        assertEquals(getSegmentSum(segments), size);


    }

    @Test
    public void getSegmentsWhenTheDefaultSegmentSizeIsMoreThanTheSegmentMaxSize() {
        BasicSegmentCalculator segmentCalculator = new BasicSegmentCalculator();
        long size = 5 * 1024 * 1024 * 1024L + (1024 * 1024);

        DownloadInformation file = get(true,size); //5gb + 1mb
        DownloaderConfiguration configuration = get(2 * 1024 * 1024,500 * 1024 * 1024,5); //max size at 500mb
        List<Segment> segments = segmentCalculator.getSegments(configuration.getMaxConcurrency(), configuration, file);

        assertEquals(11, segments.size());

        long segSize = size/11;
        assertEquals(10, segments.get(10).getSegmentIndex());
        assertEquals(segments.get(10).getInitialStartPosition(), segSize * 10);
        assertEquals(segments.get(10).getEndPosition(), size - 1);
        assertEquals(getSegmentSum(segments), size);


    }

    public DownloadInformation get(boolean acceptRange,long size)
    {
        return new DownloadInformation("",acceptRange,size);
    }
    public DownloaderConfiguration get(final long minSize, final long maxSize, final int maxConcurrency)
    {
        return new DownloaderConfiguration() {
            @Override
            public long getSegmentMinSize() {
                return minSize;
            }

            @Override
            public long getSegmentMaxSize() {
                return  maxSize;
            }

            @Override
            public int getMaxConcurrency() {
                return maxConcurrency;
            }

            @Override
            public int getMaxRetry() {
                return 3;
            }
        };
    }
}