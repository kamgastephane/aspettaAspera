package agoda.downloader;

import agoda.storage.Storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ControllerStatus {

    private HashMap<Integer, Segment> segmentList;

    private HashMap<Integer, Storage> storageList;

    private HashMap<Integer, Future> tasks;

    private int concurrency;

    private ControllerStatus(HashMap<Integer, Segment> segmentList, HashMap<Integer, Storage> storageList, int concurrency) {
        this.segmentList = segmentList;
        this.storageList = storageList;
        this.concurrency = concurrency;
        this.tasks = new HashMap<>();
    }

    void updateStatus(int segmentIndex, DownloadStatus status) {
        segmentList.computeIfPresent(segmentIndex, (integer, segment) -> {
            segment.setStatus(status);
            return segment;
        });
    }

    Storage getStorage(int segmentIndex) {
        return storageList.get(segmentIndex);
    }


    /**
     * get the next segment to be fed to the {@link DownloaderRunnable} based on the concurrency allowed
     */
    List<Segment> getNext() {

        Map<DownloadStatus, List<Segment>> segmentsGroupByState = segmentList.values().stream().collect(Collectors.groupingBy(Segment::getStatus));
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

    boolean areAllDownloadsRelatedToFinished(String url) {
        List<Segment> segments = getSegmentRelatedTo(url);
        return segments.stream().allMatch(Segment::isFinished);
    }

    List<Segment> getIdles() {
        return segmentList.values().stream().filter(Segment::isIdle).collect(Collectors.toList());
    }

    List<Segment> getDownloading() {
        return segmentList.values().stream().filter(Segment::isDownloading).collect(Collectors.toList());
    }

    Future getFutureRelatedTo(int segmentId) {
        return tasks.get(segmentId);
    }

    void addFutureRelatedTo(int segmentId, Future future) {
        tasks.put(segmentId, future);
    }

    List<Segment> getSegmentRelatedTo(String url) {
        return segmentList.values().stream().filter(segment -> segment.getSrcUrl().equals(url)).collect(Collectors.toList());
    }

    Segment getSegment(int segmentId) {
        return segmentList.get(segmentId);
    }

    void incConcurrency() {
        concurrency++;
    }

    void decConcurrency() {
        concurrency--;
    }

    static class Builder {
        private HashMap<Integer, Segment> segmentList = new HashMap<>();
        private HashMap<Integer, Storage> storageList = new HashMap<>();
        private int concurrency = 0;

        Builder init(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        Builder add(Segment segment, Storage storage) {
            int key = segment.getSegmentIndex();
            segmentList.putIfAbsent(key, segment);
            storageList.putIfAbsent(key, storage);

            return this;
        }

        ControllerStatus build() {
            if (concurrency == 0)
                throw new IllegalArgumentException("concurrency for downloader should be greater than 0");
            return new ControllerStatus(segmentList, storageList, concurrency);
        }


    }
}
