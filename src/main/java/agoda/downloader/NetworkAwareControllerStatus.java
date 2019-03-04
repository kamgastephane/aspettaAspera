package agoda.downloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * this controller status use the download speed of the previous segments to determine how
 * many new segments should be fed to the download runnable
 * this should in theory help slower networks;
 * THe steps are the following =>
 *                              - we initiate with one segment which downloads until it finishes
 *                              - we launch two segments,they download alone until they finish
 *                              - we compare the average speed of the two previous group of downloads, which initially are the first vs
 *                                  (the second and the third)
 *                              - if by passing from one segment to two concurrent segments, i reduce the download rate up
 *                                   by a certain threshold, it means i did not gain anything from the concurrency, i may as well reduce it
 *                                   for the next iteration, i will go back to the previous number of concurrent segments
 *                              - if by passing from one segment to two concurrent segments, i maintain the download rate up
 *                                   to a certain threshold, it means i gained some time by using the concurrency, i may as well keep
 *                                   increasing the concurrency to gain more for the next iterations
 */
public class NetworkAwareControllerStatus extends ControllerStatus{

    private List<List<Integer>> concurrentSegmentGroup;
    private int THRESHOLD = 20;
    private int STEP = 1;
    private static int INITIAL_INCREMENT = 2;


    public NetworkAwareControllerStatus(ControllerStatus status) {
        super(status.getSegmentList(), status.getStorageList(), status.concurrency);
        concurrentSegmentGroup = new ArrayList<>();
    }

    @Override
    List<Segment> getNext() {

        Map<DownloadStatus, List<Segment>> segmentsGroupByState = this.getSegmentList().values().stream().collect(Collectors.groupingBy(Segment::getStatus));

        if(segmentsGroupByState.size()==1 && segmentsGroupByState.containsKey(DownloadStatus.IDLE))
        {
            //all segments are idle i send the first one to download

            return getIdleToLaunch(segmentsGroupByState,1);



        }else if(segmentsGroupByState.size() > 1)
        {

            //i have segments already downloading
            List<Segment> downloadingSegments = segmentsGroupByState.get(DownloadStatus.DOWNLOADING);
            int downloading = downloadingSegments == null? 0:downloadingSegments.size();

            int allowedSize = concurrency - downloading;

            if (allowedSize > 0) {
                //i may launch some more runnable

                //i fetch the segments in the current group as two groups cannot be concurrent and downloading at the same time
                List<Integer> currentGroupIndexes = concurrentSegmentGroup.get(concurrentSegmentGroup.size() - 1);

                //if all the segment in the current group are already finished i can check what is the average rate of the download
                boolean finished = currentGroupIndexes.stream().allMatch(index -> getSegment(index).isFinished());
                List<Double> rates = new ArrayList<>();


                if(finished)
                {
                    if (currentGroupIndexes.size() ==1) {
                        //i had only the initial segment downloading
                        // i go with two segments
                        return getIdleToLaunch(segmentsGroupByState,INITIAL_INCREMENT);
                    }else if (currentGroupIndexes.size() > 1){
                        // i have some previous segment downloaded which can help me define if i should increment o reduce the number of concurrent download
                        double current= getAverageRateForGroup(concurrentSegmentGroup.size() - 1);

                        double previous = getAverageRateForGroup(concurrentSegmentGroup.size() - 2);

                        int currentSize = concurrentSegmentGroup.get(concurrentSegmentGroup.size() - 1).size();

                        int previousSize = concurrentSegmentGroup.get(concurrentSegmentGroup.size() - 2).size();



                        double expectedCurrent = previousSize *previous /currentSize;

                        if(expectedCurrent - current > (expectedCurrent*THRESHOLD/100))
                        {
                            //bad real bad, by increasing the segments downloading at the same time i  messed up the download rate
                            //the rate went down more that 10 % on the expected one
                            // i will use the previous size for the next downloads
                            int segmentCountToLaunch = Math.min(previousSize,allowedSize);
                            return getIdleToLaunch(segmentsGroupByState,segmentCountToLaunch);

                        }else if(current - expectedCurrent > (expectedCurrent*THRESHOLD/100)){
                            //the rate is better than expected i can increase the size of the concurrent downloads
                            int segmentCountToLaunch = Math.min(currentSize+STEP,allowedSize);
                            return getIdleToLaunch(segmentsGroupByState,segmentCountToLaunch);
                        }else {
                            // the rate is pretty constant, i do not change anything
                            int segmentCountToLaunch = Math.min(currentSize,allowedSize);
                            return getIdleToLaunch(segmentsGroupByState,segmentCountToLaunch);
                        }
                    }
                }


            }
        }
        return Collections.emptyList();

    }
   private List<Segment> getIdleToLaunch(Map<DownloadStatus, List<Segment>> segmentsGroupByState,int size)
   {
       List<Segment> idleSegments = segmentsGroupByState.get(DownloadStatus.IDLE);
       if(idleSegments!=null)
       {
           List<Segment> next = idleSegments.subList(0, size);
           next.forEach(Segment::downloading);
           concurrentSegmentGroup.add(next.stream().map(Segment::getSegmentIndex).collect(Collectors.toList()));

           return next;
       }
       return Collections.emptyList();

   }
    private double getAverageRateForGroup(int group)
    {
        double sum = 0;

        List<Integer> currentGroupIndexes = concurrentSegmentGroup.get(group);
        for (Integer index : currentGroupIndexes) {
            Segment segment = getSegment(index);
            sum+= segment.getRate();

        }
        return sum/currentGroupIndexes.size();
    }


}
