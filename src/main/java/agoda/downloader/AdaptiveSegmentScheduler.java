package agoda.downloader;

import java.util.*;
import java.util.stream.Collectors;


/**
 * this scheduler uses the download speed of the previous segments to determine how
 * many new segments should be fed to the download runnable
 * this should in "theory" help slower networks
 * The steps could be the following =>
 *                              - we initiate with one segment which downloads until it finishes
 *                              - we launch two segments,they download alone until they finish
 *                              - we compare the average speed of the two previous group of downloads, which initially are the first vs
 *                                  (the second and the third)
 *                              - if by passing from one segment to two concurrent segments, i reduce the download rate lower than
 *                                   the previous by certain threshold, it means i did not gain anything from the concurrency,
 *                                   i may as well reduce the number of concurrent segments for the next iteration
 *                              - if by passing from one segment to two concurrent segments, i maintain the download rate up
 *                                   to a certain threshold, it means i gained some time by using the concurrency, i may as well keep
 *                                   increasing the concurrency to gain more for the next iterations
 *                              - and so on
 */
public class AdaptiveSegmentScheduler implements SegmentScheduler {

    private List<SegmentGroup> concurrentSegmentGroup;
    private int downloadRateDiffIncreaseThreshold = 10;
    private int downloadRateDiffDecreaseThreshold = 40;

    private int segmentGroupIncreaseSizeStep = 1;
    private int segmentGroupDecreaseSizeStep = 1;

    private static int segmentGroupInitialSize = 1;

    public AdaptiveSegmentScheduler(int downloadRateDiffIncreaseThreshold, int downloadRateDiffDecreaseThreshold, int segmentGroupIncreaseSizeStep, int segmentGroupDecreaseSizeStep) {
        concurrentSegmentGroup = new ArrayList<>();
        this.downloadRateDiffIncreaseThreshold = downloadRateDiffIncreaseThreshold;
        this.downloadRateDiffDecreaseThreshold = downloadRateDiffDecreaseThreshold;
        this.segmentGroupIncreaseSizeStep = segmentGroupIncreaseSizeStep;
        this.segmentGroupDecreaseSizeStep = segmentGroupDecreaseSizeStep;
    }

    public AdaptiveSegmentScheduler() {
        concurrentSegmentGroup = new ArrayList<>();
    }

    @Override
    public List<Segment> getNext(Collection<Segment> segmentsCollection, int concurrency) {

        List<Segment> segments = new ArrayList<>(segmentsCollection);
        Map<DownloadStatus, List<Segment>> segmentsGroupByState = segments.stream().collect(Collectors.groupingBy(Segment::getStatus));

        if(segmentsGroupByState.size()==1 && segmentsGroupByState.containsKey(DownloadStatus.IDLE))
        {
            //all segments are idle i send the first one to download
            return getIdleToLaunch(segmentsGroupByState,segmentGroupInitialSize,null);

        }else if(segmentsGroupByState.size() > 1)
        {
            //i have segments already downloading
            List<Segment> downloadingSegments = segmentsGroupByState.get(DownloadStatus.DOWNLOADING);
            int downloading = downloadingSegments == null? 0:downloadingSegments.size();

            int scheduled = concurrency - downloading;

            //scheduled is the number of new segments i could launch while still respecting the requested concurrency
            if (scheduled > 0) {

                //i fetch the segments in the recent concurrentSegmentGroup
                SegmentGroup segmentGroup = concurrentSegmentGroup.get(concurrentSegmentGroup.size() - 1);

                //if all the segment in the current group are  finished i can check what is the average rate of the download
                boolean finished = segmentGroup.isSegmentsFinished(segments);

                if(finished)
                {
                    if (concurrentSegmentGroup.size() ==1) {

                        //i had only the initial segment downloading
                        // i go with the size of the previous group plus the segmentGroupSizeStep
                        return getIdleToLaunch(segmentsGroupByState,concurrentSegmentGroup.get(0).getSize() + segmentGroupIncreaseSizeStep,0);

                    }else if (concurrentSegmentGroup.size() > 1){

                        return scheduleBasedOnPreviousDownloadRate(scheduled, segmentsGroupByState);
                    }
                }

            }
        }
        return Collections.emptyList();


    }


    private List<Segment> scheduleBasedOnPreviousDownloadRate(int concurrency, Map<DownloadStatus, List<Segment>> segmentsGroupByState)
    {
        // i have some previous segment finished which can help me define if i should increment o reduce the number of concurrent download

        //TODO use the groupRelated instead of the previous group
        double currentRate = concurrentSegmentGroup.get(concurrentSegmentGroup.size()-1).rate;

        double previousRate = concurrentSegmentGroup.get(concurrentSegmentGroup.size()-2).rate;

        int currentSize = concurrentSegmentGroup.get(concurrentSegmentGroup.size()-1).getSize();

        int previousSize = concurrentSegmentGroup.get(concurrentSegmentGroup.size()-2).getSize();


        double expectedCurrent = previousSize *previousRate /currentSize;

        double incStep = expectedCurrent * downloadRateDiffIncreaseThreshold/100;
        double decStep = expectedCurrent * downloadRateDiffDecreaseThreshold/100;

        int relatedId = concurrentSegmentGroup.size()-1;
        if(expectedCurrent - currentRate > decStep)
        {
            //the rate went down more that "downloadRateDiffThreshold" % on the expected one
            // i will decrease for the next downloads
            int nextSize = Math.max(1,currentSize - segmentGroupDecreaseSizeStep);
            nextSize = Math.min(nextSize,concurrency);
            return getIdleToLaunch(segmentsGroupByState,nextSize,relatedId);

        }else if(currentRate - expectedCurrent > incStep)
        {
            //the rate is better than expected i can increase the size of for the next downloads
            int nextSize = currentSize + segmentGroupIncreaseSizeStep;
            nextSize = Math.min(nextSize,concurrency);
            return getIdleToLaunch(segmentsGroupByState,nextSize,relatedId);

        }else {
            // the rate is pretty constant, i do not change anything
             relatedId = concurrentSegmentGroup.size()-2;

            int segmentCountToLaunch = Math.min(currentSize,concurrency);
            return getIdleToLaunch(segmentsGroupByState,segmentCountToLaunch,relatedId);

        }
    }

    private List<Segment> getIdleToLaunch(Map<DownloadStatus, List<Segment>> segmentsGroupByState,int size,Integer groupRelated)
    {
        List<Segment> idleSegments = segmentsGroupByState.get(DownloadStatus.IDLE);
        if(idleSegments!=null)
        {
            List<Segment> next = idleSegments.subList(0, Math.min(size,idleSegments.size()));
            next.forEach(Segment::downloading);
            concurrentSegmentGroup.add(new SegmentGroup(next,groupRelated));
            return next;
        }
        return Collections.emptyList();

    }

    private class SegmentGroup{
        List<Integer> segmentIds;
        double rate;
        Integer relatedTo;

        SegmentGroup(Collection<Segment> segmentList,Integer groupRelated) {
            this.segmentIds = segmentList.stream().map(Segment::getSegmentIndex).collect(Collectors.toList());
            this.relatedTo = groupRelated;
        }

        int getSize()
        {
            return segmentIds.size();
        }
        boolean isSegmentsFinished(List<Segment> segmentList)
        {
            boolean finished = true;
            double sum = 0;

            for (Integer index : segmentIds) {

                Segment segment = segmentList.get(index);
                sum+= segment.getRate();
                if(!segment.isFinished())
                {
                    finished = false;
                    break;
                }
            }
            if(finished)
            {
                rate = sum/segmentIds.size();
            }
            return finished;
        }
    }
}
