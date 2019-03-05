package agoda.downloader;

import agoda.storage.StorageSupplier;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ControllerStatus {

    private HashMap<Integer, Segment> segmentList;

    private HashMap<Integer, StorageSupplier> storageList;

    private HashMap<Integer, Future> tasks;

    private int concurrency;

    private SegmentScheduler segmentScheduler;

    public ControllerStatus(int concurrency,SegmentScheduler segmentScheduler) {

        if (concurrency == 0)
            throw new IllegalArgumentException("concurrency for downloader should be greater than 0");
        this.concurrency = concurrency;
        this.segmentList = new HashMap<>();
        this.storageList = new HashMap<>();
        this.tasks = new HashMap<>();
        this.segmentScheduler = segmentScheduler;
    }

    public void  add(Segment segment, StorageSupplier storageSupplier)
    {
        int key = segment.getSegmentIndex();
        segmentList.putIfAbsent(key, segment);
        storageList.putIfAbsent(key, storageSupplier);
    }

    void updateStatus(int segmentIndex, DownloadStatus status) {
        segmentList.computeIfPresent(segmentIndex, (integer, segment) -> {
            segment.setStatus(status);
            return segment;
        });
    }
    //TODO fix this
    HashMap<Integer, StorageSupplier> getStorageList() {
        return storageList;
    }


    //TODO fix this
    HashMap<Integer, Segment> getSegmentList() {
        return segmentList;
    }

    StorageSupplier getStorage(int segmentIndex) {
        return storageList.get(segmentIndex);
    }


    /**
     * get the next segment to be fed to the {@link DownloaderRunnable} based on the concurrency allowed
     */
    List<Segment> getNext() {

        return this.segmentScheduler.getNext(segmentList.values(),concurrency);
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
        concurrency = Math.max(1,concurrency);
    }



}
