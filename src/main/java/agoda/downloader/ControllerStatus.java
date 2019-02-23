package agoda.downloader;

import agoda.storage.Storage;

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
    public void updateStatus(int segmentIndex,DownloadStatus status)
    {
        segmentList.computeIfPresent(segmentIndex,(integer, segment) ->{
            segment.setStatus(status);
            return segment;
        });
    }
    public Storage getStorage(int segmentIndex)
    {
        return storageList.get(segmentIndex);
    }

    public boolean hasIdle()
    {
        return segmentList.values().stream().anyMatch(Segment::isIdle);
    }

    /**
     * get the next segment to be fed to the {@link DownloaderRunnable} based on the concurrency allowed
     */
    public List<Segment> getNext()
    {

        Map<DownloadStatus, List<Segment>> collect = segmentList.values().stream().collect(Collectors.groupingBy(Segment::getStatus));
        int downloading = collect.get(DownloadStatus.DOWNLOADING).size();
        int nextSize = concurrency - downloading;
        if(nextSize > 0)
        {
            //i can launch some more runnable
            List<Segment> next = collect.get(DownloadStatus.IDLE).subList(0,nextSize);
            next.forEach(Segment::downloading);
            return next;
        }
        return null;
    }

    public Future getFutureRelatedTo(int segmentId)
    {
        return tasks.get(segmentId);
    }
    public void addFutureRelatedTo(int segmentId,Future future)
    {
        tasks.put(segmentId,future);
    }
    public List<Segment> getSegmentRelatedTo(String url)
    {
        return segmentList.values().stream().filter(segment -> segment.getSrcUrl().equals(url)).collect(Collectors.toList());
    }

    public void incConcurrency()
    {
        concurrency++;
    }
    public void decConcurrency()
    {
        concurrency--;
    }
    public static class Builder{
        private HashMap<Integer, Segment> segmentList = new HashMap<>();
        private HashMap<Integer, Storage> storageList = new HashMap<>();
        private int concurrency = 0;
        public Builder init(int concurrency)
        {
            this.concurrency = concurrency;
            return this;
        }
        public Builder add(Segment segment,Storage storage)
        {
            int key = segment.getSegmentIndex();
            segmentList.putIfAbsent(key,segment);
            storageList.putIfAbsent(key,storage);

            return this;
        }
        public ControllerStatus build()
        {
            if(concurrency == 0)throw new IllegalArgumentException("concurrency for downloader should be greater than 0");
            return new ControllerStatus(segmentList,storageList,concurrency);
        }






    }
}
