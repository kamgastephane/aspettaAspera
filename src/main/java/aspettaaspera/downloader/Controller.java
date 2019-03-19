package aspettaaspera.downloader;

import aspettaaspera.configuration.Configuration;
import aspettaaspera.downloader.messaging.DownloaderMessage;
import aspettaaspera.downloader.messaging.ResultMessage;
import aspettaaspera.protocols.ProtocolHandler;
import aspettaaspera.storage.Storage;
import aspettaaspera.storage.StorageFactory;
import aspettaaspera.storage.StorageSupplier;
import org.apache.commons.io.FileUtils;
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
    private Configuration config;
    private ProtocolHandler protocolHandler;
    private SegmentDivider segmentDivider;
    private ResourceInformation resourceInformation;
    private LinkedBlockingQueue<ResultMessage> queue;
    private HashMap<String,String> resourceParameters;
    private StorageFactory storageFactory;
    private double segmentSizeSaved = 0;

    //TODO handle list of differents URLs...It should be straight forward at this point
    public Controller(String url, HashMap<String,String> resourceParams, Configuration ctrlConfig, int maxConcurrentDownload,
                      SegmentDivider segmentDivider, ProtocolHandler handler,
                      StorageFactory storageFactory
                     ) {
        this.maxConcurrentDownload = maxConcurrentDownload;
        this.url = url;
        this.config = ctrlConfig;
        this.resourceParameters = resourceParams;
        this.segmentDivider = segmentDivider;
        this.protocolHandler = handler;
        this.storageFactory = storageFactory;



    }

    Map<String, List<StorageSupplier>> initStorage(List<Segment> segments) {

        HashMap<String, List<StorageSupplier>> storageMap = new HashMap<>();
        for (Segment segment : segments) {
            StorageSupplier supplier = storageFactory.getStorage(segment.getSrcUrl(), config.getStorageConfiguration().getDownloadFolder(),
                        config.getStorageConfiguration().getOutputStreamBufferSize());

//                LazyBufferedStorage supplier =  new LazyBufferedStorage(segment.getSrcUrl(), config.getStorageConfiguration().getDownloadFolder(),
//                        config.getStorageConfiguration().getOutputStreamBufferSize());

                    if (storageMap.containsKey(segment.getSrcUrl()))
                    {
                        storageMap.get(segment.getSrcUrl()).add(supplier);
                    }else{
                        List<StorageSupplier> list = new ArrayList<>();
                        list.add(supplier);
                        storageMap.put(segment.getSrcUrl(),list);
                    }
                }
        return storageMap;
    }

    public void setup() {
        //fetch the info about the file
        resourceInformation = getFileInformation(maxConcurrentDownload);

        //create the download segments
        List<Segment> segmentList = createSegments();
        logger.info("{} segments allocated",segmentList.size());


        int effectiveConcurrency = Math.min(maxConcurrentDownload, segmentList.size());

        //create the queue
        queue = new LinkedBlockingQueue<>();

        //create the thread pool
        pool = Executors.newFixedThreadPool(effectiveConcurrency);
        logger.info("{} threads allocated",effectiveConcurrency);


        //create the storage
        Map<String, List<StorageSupplier>> storages = initStorage(segmentList);


        //create the controller status

        SegmentScheduler segmentScheduler;
        if(config.getDownloaderConfiguration().useAdaptiveScheduler()){
            segmentScheduler = new AdaptiveSegmentScheduler();
        }else {
            segmentScheduler = new BaseSegmentScheduler();
        }
        controllerStatus = new ControllerStatus(effectiveConcurrency,segmentScheduler);

        //fill the controller status

        Map<String, List<Segment>> segmentByUrl = segmentList.stream().collect(Collectors.groupingBy(Segment::getSrcUrl));

        segmentByUrl.forEach((url, segments) ->
        {
            List<StorageSupplier> storageForUrl = storages.get(url);
            if (segments.size() != storageForUrl.size()) {
                //this should never happened
                logger.fatal("This is quite unusual, i have storage not corresponding to the segments to download! Download of the url {} will be cancelled", url);
            } else {
                for (int i = 0; i < segments.size(); i++) {
                    controllerStatus.add(segments.get(i), storageForUrl.get(i));
                }
            }
        });
    }


    public void run() {

        while (controllerStatus.getDownloading().size()>0 || controllerStatus.getIdles().size()>0)
        {
            //if i have some segments still downloading or either idle i wait

            List<Segment> next = controllerStatus.getNext();
            if (next.size() > 0) {
                logger.info("Found {} new segments to download",next.size());

                Set<String> failedUrl = new HashSet<>();
                for (Segment segment : next) {

                    segment.setStatus(DownloadStatus.DOWNLOADING);

                    //we create the storage
                    Storage storage = controllerStatus.getStorage(segment.getSegmentIndex()).get();
                    if (storage == null) {
                        //i failed to create storage for this specific url to download: abort
                        logger.warn("Failed to create some storage; {} Download of the url {} will be cancelled", url);
                        failedUrl.add(segment.getSrcUrl());
                    } else{
                        //we create the runnable task
                        DownloaderRunnable runnable = new DownloaderRunnable(segment, protocolHandler, queue);
                        logger.info("Starts donwload of the segment at index {}",segment.getSegmentIndex());
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

        //i cleanup the resource
        protocolHandler.close();



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
            StorageSupplier storage = controllerStatus.getStorage(segment.getSegmentIndex());
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
                        segment.setRate(resultMessage.getRate());
                    }
            }
            if (DownloadStatus.FINISHED == resultMessage.getStatus()) {
                try {
                    if(!segment.isFinished()){
                        controllerStatus.getStorage(segment.getSegmentIndex()).get().close();
                        segment.setStatus(DownloadStatus.FINISHED);
                        String approxSize = String.format("%.2f", segmentSizeSaved / FileUtils.ONE_MB);

                        logger.info("Segment {} fully downloaded; total size downloaded: {} MB", segment.getSegmentIndex(),approxSize);

                    }

                } catch (IOException e) {
                    logger.error("error while closing the storage stream for " + segment.getSrcUrl(), e);
                    abortDownloadFor(segment.getSrcUrl());
                }
                if (controllerStatus.areAllDownloadsRelatedToFinished(segment.getSrcUrl())) {
                    try {
                        joinSegments(segment.getSrcUrl());
                        String fileName = controllerStatus.getStorage(0).get().getFileName();
                        String approxSize = String.format("%.2f", segmentSizeSaved / FileUtils.ONE_MB);
                        logger.info("{} fully downloaded; path {}; size {} MB", segment.getSrcUrl(),fileName, approxSize);

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

    public double getSegmentSizeSaved() {
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
        List<Segment> segments = segmentDivider.getSegments(maxConcurrentDownload, config.getDownloaderConfiguration(), resourceInformation);
        segments.forEach(segment -> segment.setStatus(DownloadStatus.IDLE));
        return segments;

    }

    private ResourceInformation getFileInformation(int maxAttempts) {
        //we maintain the same logic and request this a few times until declaring failure
        int attempt = 1;

        ResourceInformation base = new ResourceInformation(url, false, 0);
        while (attempt <= maxAttempts) {
            try {
                ResourceInformation information = protocolHandler.getInfo(this.url,resourceParameters);
                if(information != null)return information;
            } catch (DownloadException e) {
                logger.error(String.format("Error while retrieving the information related to %s attempt %d max attempt allowed %d", url, attempt, maxAttempts), e);
                attempt++;
            }

        }
        return base;
    }


}
