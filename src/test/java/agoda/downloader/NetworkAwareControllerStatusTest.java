package agoda.downloader;

import agoda.storage.Storage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class NetworkAwareControllerStatusTest {
    @Test
    public void testThatget_TheNextSegmentToDownload_RespectTheAllowedAccuracy()
    {
        ControllerStatus.Builder builder = new ControllerStatus.Builder();
        builder = builder.init(5);
        for (int i = 0; i < 10; i++) {
            Segment segment = new Segment.SegmentBuilder()
                    .setSegmentIndex(i)
                    .createSegment();
            segment.setStatus(DownloadStatus.IDLE);
            Storage storage = new Storage() {
                @Override
                public boolean push(byte[] buffer) {
                    return true;
                }

                @Override
                public void close() {

                }

                @Override
                public boolean reset() {
                    return false;
                }
            };

            builder.add(segment,TestUtils.getMockLazyStorage(storage));
        }
        NetworkAwareControllerStatus status = new NetworkAwareControllerStatus(builder.build());

        List<Segment> next = status.getNext();

        assertEquals(1, next.size());

        for (Segment segment : next) {
            assertEquals(segment.getStatus(), DownloadStatus.DOWNLOADING);
        }
        status.decConcurrency();
        status.decConcurrency();

        List<Segment> nothingNext = status.getNext();
        assertEquals(0, nothingNext.size());

        status.updateStatus(next.get(0).getSegmentIndex(),DownloadStatus.FINISHED);

        List<Segment> secondGroup = status.getNext();
        assertEquals(2, secondGroup.size());


        List<Segment> thirdGroup = status.getNext();
        assertEquals(0, thirdGroup.size());


        status.updateStatus(secondGroup.get(0).getSegmentIndex(),DownloadStatus.FINISHED);

        thirdGroup = status.getNext();
        assertEquals(0, thirdGroup.size());

        status.updateStatus(secondGroup.get(1).getSegmentIndex(),DownloadStatus.FINISHED);
        thirdGroup = status.getNext();
        assertEquals(2, thirdGroup.size());


        status.updateStatus(thirdGroup.get(0).getSegmentIndex(),DownloadStatus.FINISHED);
        status.updateStatus(thirdGroup.get(1).getSegmentIndex(),DownloadStatus.FINISHED);

        List<Segment> fourthGroup = status.getNext();
        assertEquals(2, fourthGroup.size());

    }


    @Test
    public void testThatget_TheNextSegmentToDownload_CorrespondTOThePreviousRate()
    {
        ControllerStatus.Builder builder = new ControllerStatus.Builder();
        builder = builder.init(5);
        for (int i = 0; i < 20; i++) {
            Segment segment = new Segment.SegmentBuilder()
                    .setSegmentIndex(i)
                    .createSegment();
            segment.setStatus(DownloadStatus.IDLE);
            Storage storage = new Storage() {
                @Override
                public boolean push(byte[] buffer) {
                    return true;
                }

                @Override
                public void close() {

                }

                @Override
                public boolean reset() {
                    return false;
                }
            };

            builder.add(segment,TestUtils.getMockLazyStorage(storage));
        }
        NetworkAwareControllerStatus status = new NetworkAwareControllerStatus(builder.build());

        List<Segment> firstGroup = status.getNext();

        assertEquals(1, firstGroup.size());

        setFinishedWithRate(firstGroup,100);

        List<Segment> secondGroup = status.getNext();
        assertEquals(2, secondGroup.size());


        List<Segment> thirdGroup = status.getNext();
        assertEquals(0, thirdGroup.size());


        setFinishedWithRate(secondGroup.subList(0,1),100);

        thirdGroup = status.getNext();
        assertEquals(0, thirdGroup.size());

        setFinishedWithRate(secondGroup,100);

        //the second group is over with rate equals to the first group i should have an increments in the next group size

        thirdGroup = status.getNext();
        assertEquals(3, thirdGroup.size());

        setFinishedWithRate(thirdGroup,50);

        //the third group performed poorly i should regress to two segments

        List<Segment> fourthGroup = status.getNext();
        assertEquals(2, fourthGroup.size());

        status.decConcurrency();
        status.decConcurrency();
        status.decConcurrency();
        status.decConcurrency();

        //i set the concurrency at one now

        setFinishedWithRate(fourthGroup,100);

        // i performed well again with two elements i can increase but the accuracy is limited to 1
        List<Segment> fifthGroup = status.getNext();
        assertEquals(1, fifthGroup.size());

        setFinishedWithRate(fifthGroup,100);
        status.incConcurrency();
        status.incConcurrency();

        // i performed well again with  elements i can increase
        List<Segment> sixth = status.getNext();
        assertEquals(2, sixth.size());
        setFinishedWithRate(sixth,61);

        // i performed well again with  elements i can increase
        List<Segment> seventh = status.getNext();
        assertEquals(3, seventh.size());



    }

    private void setFinishedWithRate(List<Segment> segments, double rate)
    {
        segments.forEach(segment -> {
            segment.setStatus(DownloadStatus.FINISHED);
            segment.setRate(rate);
        });
    }


}