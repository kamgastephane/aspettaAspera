package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.downloader.messaging.DownloaderMessage;
import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ProtocolHandler;
import agoda.storage.LazyStorage;
import agoda.storage.Storage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Controller {

    private static final Logger logger = LogManager.getLogger();
    ExecutorService pool;
    ControllerStatus controllerStatus;
    private int maxConcurrentDownload;
    private String url;
    private Configuration configuration;
    private ProtocolHandler protocolHandler;
    private SegmentsCalculator segmentsCalculator;
    private DownloadInformation downloadInformation;
    private LinkedBlockingQueue<ResultMessage> queue;
    private long segmentSizeSaved = 0;

    public Controller(String url,Configuration configuration, int maxConcurrentDownload,SegmentsCalculator segmentsCalculator,ProtocolHandler handler) {
        this.maxConcurrentDownload = maxConcurrentDownload;
        this.url = url;
        this.configuration = configuration;


        this.segmentsCalculator = segmentsCalculator;
        this.protocolHandler = handler;

    }

    Map<String, List<LazyStorage>> initStorage(List<Segment> segments) {

        HashMap<String, List<LazyStorage>> storageMap = new HashMap<>();
        for (Segment segment : segments) {

                LazyStorage supplier =  new LazyStorage(segment.getSrcUrl(), configuration.getStorageConfiguration().getDownloadFolder(),
                        configuration.getStorageConfiguration().getOutputStreamBufferSize());

                    if (storageMap.containsKey(segment.getSrcUrl()))
                    {
                        storageMap.get(segment.getSrcUrl()).add(supplier);
                    }else{
                        List<LazyStorage> list = new ArrayList<>();
                        list.add(supplier);
                        storageMap.put(segment.getSrcUrl(),list);
                    }
                }
        return storageMap;
    }

    void setup() {
        //fetch the info about the file
        downloadInformation = getFileInformation(maxConcurrentDownload);

        //create the download segments
        List<Segment> segmentList = createSegments();


        int effectiveConcurrency = Math.min(maxConcurrentDownload, segmentList.size());

        //create the queue
        queue = new LinkedBlockingQueue<>();

        //create the thread pool
        pool = Executors.newFixedThreadPool(effectiveConcurrency);


        //create the storage
        Map<String, List<LazyStorage>> storages = initStorage(segmentList);


        //create the controller status
        Map<String, List<Segment>> segmentByUrl = segmentList.stream().collect(Collectors.groupingBy(Segment::getSrcUrl));

        ControllerStatus.Builder builder = new ControllerStatus.Builder().init(effectiveConcurrency);

        segmentByUrl.forEach((url, segments) ->
        {
            List<LazyStorage> storageForUrl = storages.get(url);
            if (segments.size() != storageForUrl.size()) {
                //this should never happened
                logger.fatal("This is quite unusual, i have storage not corresponding to the segments to download! Download of the url {} will be cancelled", url);
            } else {
                for (int i = 0; i < segments.size(); i++) {
                    builder.add(segments.get(i), storageForUrl.get(i));
                }
            }
        });
        controllerStatus = builder.build();
    }


    public void run() {

        while (controllerStatus.getDownloading().size()>0 || controllerStatus.getIdles().size()>0)
        {
            //if i have some segments still downloading or either idle i wait

            List<Segment> next = controllerStatus.getNext();
            if (next.size() > 0) {
                //we create the runnable task
                Set<String> failedUrl = new HashSet<>();
                for (Segment segment : next) {
                    //the protocol handler cannot be null because of the constructor check
                    segment.setStatus(DownloadStatus.DOWNLOADING);

                    //we create the storage
                    Storage storage = controllerStatus.getStorage(segment.getSegmentIndex()).get();
                    if (storage == null) {
                        //i failed to create storage for this specific url to download: abort
                        logger.info("Failed to create some storage; {} Download of the url {} will be cancelled", url);
                        failedUrl.add(segment.getSrcUrl());
                    } else{
                        DownloaderRunnable runnable = new DownloaderRunnable(segment, protocolHandler, queue);
                        Future future = pool.submit(runnable);
                        controllerStatus.addFutureRelatedTo(segment.getSegmentIndex(), future);
                    }


                }
                //we cleanup all the failed url
                failedUrl.forEach(this::abortDownloadFor);
            }

            try {
                ResultMessage message = queue.take();
                handleMessage(message);
            } catch (InterruptedException e) {
                //something really bad happened
                //i can cleanup all unfinished download

                List<Segment> idleSegment = controllerStatus.getIdles();
                List<Segment> downloadingSegment = controllerStatus.getDownloading();

                abortDownloadFor(idleSegment);
                abortDownloadFor(downloadingSegment);

            }
        }
        //i'm done => all segments downloaded or al in error i will return




    }

    /**
     * called when the download failed or an exception happened
     *
     * @param segments the list of segment we want to reset
     */
    private void abortDownloadFor(List<Segment> segments) {
        for (Segment segment : segments) {
            //interrupt all running thread related to this if they are still downloading
            segment.setStatus(DownloadStatus.ERROR);

            Future task = controllerStatus.getFutureRelatedTo(segment.getSegmentIndex());
            if(task!=null && !task.isCancelled())task.cancel(true);

            //clean all storage
            LazyStorage storage = controllerStatus.getStorage(segment.getSegmentIndex());
            if(storage.isInit())
            {
                storage.get().reset();
            }
        }
    }

    /**
     * called when the download failed or an exception happened
     *
     * @param url the url we want to stop downloading
     */
    private void abortDownloadFor(String url) {
        List<Segment> segmentRelatedTo = controllerStatus.getSegmentRelatedTo(url);
        abortDownloadFor(segmentRelatedTo);

    }
    private void handleMessage(DownloaderMessage message) {
        Segment segment = controllerStatus.getSegment(message.getSegmentId());

        if (DownloaderMessage.Type.RESULT == message.getType()) {
            ResultMessage resultMessage = (ResultMessage) message;
            //if the task is still downloading or finished and it is not in error, we saved the bytes received
            if (DownloadStatus.DOWNLOADING == resultMessage.getStatus() ||
                    DownloadStatus.FINISHED == resultMessage.getStatus()) {
                    if(resultMessage.getContent()!=null && resultMessage.getContent().length>0){
                        Storage storage = controllerStatus.getStorage(message.getSegmentId()).get();
                        storage.push(resultMessage.getContent());
                        segmentSizeSaved +=resultMessage.getContent().length;

                        //we update the downloadRate for this segment
                        if(resultMessage.getRate()>0)segment.setRate(resultMessage.getRate());
                    }
            }
            if (DownloadStatus.FINISHED == resultMessage.getStatus()) {
                try {
                    controllerStatus.getStorage(segment.getSegmentIndex()).get().close();
                    segment.setStatus(DownloadStatus.FINISHED);

                } catch (IOException e) {
                    logger.error("error while closing the storage stream for " + segment.getSrcUrl(), e);
                    abortDownloadFor(segment.getSrcUrl());
                }
                if (controllerStatus.areAllDownloadsRelatedToFinished(segment.getSrcUrl())) {
                    try {
                        joinSegments(segment.getSrcUrl());
                        logger.info("{} fully downloaded at with {} kbs", segment.getSrcUrl(), segmentSizeSaved /1024);

                    } catch (IOException e) {
                        logger.error("error while joining the storage stream for " + segment.getSrcUrl(), e);
                        abortDownloadFor(segment.getSrcUrl());

                    }
                }
            } else if (DownloadStatus.ERROR == resultMessage.getStatus()) {
                abortDownloadFor(segment.getSrcUrl());
            }
        }

    }

    public long getSegmentSizeSaved() {
        return segmentSizeSaved;
    }

    /**
     * call when all segments finished downloading
     *
     * @param url the url of the finished download
     */
    private void joinSegments(String url) throws IOException {
        List<Segment> segments = controllerStatus.getSegmentRelatedTo(url);
        if(segments.size()>1)
        {
            //we order the segmentlist based on the the segmentIndex
            Collections.sort(segments);

            //we fetch all the storage related
            List<Storage> storageList = new ArrayList<>();
            segments.forEach(segment -> storageList.add(controllerStatus.getStorage(segment.getSegmentIndex()).get()));


            Storage.join(storageList,true);
        }


    }

    private List<Segment> createSegments() {
        List<Segment> segments = segmentsCalculator.getSegments(maxConcurrentDownload, configuration.getDownloaderConfiguration(), downloadInformation);
        segments.forEach(segment -> segment.setStatus(DownloadStatus.IDLE));
        return segments;

    }

    private DownloadInformation getFileInformation(int maxAttempts) {
        //we maintain the same logic and request this a few time until declaring failure
        int attempt = 1;

        DownloadInformation info = new DownloadInformation(url, false, 0);
        while (attempt <= maxAttempts) {
            try {
                return protocolHandler.getInfo(this.url);

            } catch (DownloadException e) {
                logger.error(String.format("Error while retrieving the information related to %s attempt %d max attempt allowed %d", url, attempt, maxAttempts), e);
                attempt++;
            }

        }
        return info;
    }


}
