package agoda.downloader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseSegmentScheduler implements SegmentScheduler {


    @Override
    public List<Segment> getNext(Collection<Segment> segments, int concurrency)
    {
        Map<DownloadStatus, List<Segment>> segmentsGroupByState = segments.stream().collect(Collectors.groupingBy(Segment::getStatus));
        List<Segment> downloadingSegments = segmentsGroupByState.get(DownloadStatus.DOWNLOADING);
        int downloading = downloadingSegments == null? 0:downloadingSegments.size();
        int nextSize = concurrency - downloading;
        if (nextSize > 0) {
            //i can launch some more runnable
            List<Segment> idleSegments = segmentsGroupByState.get(DownloadStatus.IDLE);
            if(idleSegments!=null)
            {
                List<Segment> next = idleSegments.subList(0, nextSize);
                next.forEach(Segment::downloading);
                return next;
            }
        }
        return Collections.emptyList();
    }
}
