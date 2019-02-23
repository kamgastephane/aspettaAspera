package agoda.downloader;

import agoda.storage.Storage;
import org.junit.Assert;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class ControllerStatusTest {


    public void getTheNextSegmentToDownloadRespectToTheAllowedAccuracy()
    {
        ControllerStatus.Builder builder = new ControllerStatus.Builder();
        builder = builder.init(5);
        for (int i = 0; i < 10; i++) {
            Segment segment = new Segment.SegmentBuilder()
                    .setSegmentIndex(i)
                    .createSegment();
            Storage storage = new Storage() {
                @Override
                public boolean push(byte[] buffer) {
                    return true;
                }

                @Override
                public void close() throws IOException {

                }
            };
            builder.add(segment,storage);
        }
        ControllerStatus status = builder.build();
        List<Segment> next = status.getNext();
        assertEquals(5, next.size());
        for (Segment segment : next) {
            assertEquals(segment.getStatus(), DownloadStatus.DOWNLOADING);
        }
        status.decConcurrency();
        status.decConcurrency();

        List<Segment> nothingNext = status.getNext();
        assertEquals(0, nothingNext.size());

        status.updateStatus(next.get(0).getSegmentIndex(),DownloadStatus.FINISHED);
        status.updateStatus(next.get(1).getSegmentIndex(),DownloadStatus.FINISHED);
        nothingNext = status.getNext();
        assertEquals(0, nothingNext.size());

        status.updateStatus(next.get(2).getSegmentIndex(),DownloadStatus.FINISHED);
        nothingNext = status.getNext();
        assertEquals(1, nothingNext.size());

        status.incConcurrency();
        status.updateStatus(next.get(3).getSegmentIndex(),DownloadStatus.FINISHED);
        status.updateStatus(next.get(4).getSegmentIndex(),DownloadStatus.FINISHED);
        next = status.getNext();
        assertEquals(3, next.size());
    }

}