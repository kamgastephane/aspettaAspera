package agoda.downloader;

import agoda.configuration.DownloaderConfiguration;

import java.util.ArrayList;
import java.util.List;

public class BasicSegmentCalculator implements SegmentsCalculator {

    private BasicSegmentCalculator() {
    }

    static BasicSegmentCalculator getInstance() {
        return BasicSegmentCalculatorSingleton.INSTANCE;
    }
    private static class BasicSegmentCalculatorSingleton {
        private static final BasicSegmentCalculator INSTANCE = new BasicSegmentCalculator();
    }
    @Override
    public List<Segment> getSegments(int desiredSegmentCount, DownloaderConfiguration configuration, DownloadInformation information) {


        List<Segment> segments = new ArrayList<>();
        int count = desiredSegmentCount;

        if (!information.isAcceptRange() || information.getSize() == 0) {

            // we do not care anymore about the segment as the range request is not supported, we will try a single threaded download
            Segment unique = new Segment.SegmentBuilder()
                    .setSegmentIndex(0)
                    .setEndPosition(information.getSize() - 1)
                    .setMaxRetry(configuration.getMaxRetry())
                    .setSrcUrl(information.getSrcUrl())
                    .setRequestRange(0)
                    .createSegment();
            segments.add(unique);

        } else if (information.isAcceptRange() && information.getSize() > 0) {
            long segmentSize = information.getSize() / (long) count;
            if (segmentSize > configuration.getSegmentMaxSize()) {
                //we try to increase the number of segment
                while (segmentSize > configuration.getSegmentMaxSize()) {
                    count++;
                    segmentSize = information.getSize() / (long) count;

                }

            } else if (segmentSize < configuration.getSegmentMinSize()) {
                //we try to reduce the number of segment
                while (count > 1 && segmentSize < configuration.getSegmentMinSize()) {
                    count--;
                    segmentSize = information.getSize() / (long) count;

                }
            }
            long start = 0;
            for (int i = 0; i < count; i++) {

                long end = start + segmentSize;
                if (i == count - 1) {
                    //for the last segment, we go until the end of the file
                    end = information.getSize();
                }
                Segment segment = new Segment.SegmentBuilder()
                        .setSegmentIndex(i)
                        .setInitialStartPosition(start)
                        .setEndPosition(end - 1)
                        .setMaxRetry(configuration.getMaxRetry())
                        .setSrcUrl(information.getSrcUrl())
                        .setRequestRange(configuration.getChunkSize())
                        .createSegment();
                segments.add(segment);
                start += segmentSize;
            }

        }
        return segments;

    }


}
