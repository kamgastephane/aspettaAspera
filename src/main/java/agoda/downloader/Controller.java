package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.downloader.messaging.DownloaderMessage;
import agoda.downloader.messaging.ResultMessage;
import agoda.protocols.ProtocolHandler;
import agoda.protocols.ProtocolHandlerFactory;
import agoda.storage.Storage;
import agoda.storage.StorageFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Controller {

    private int maxConcurrentDownload;
    private String url;
    private Configuration configuration;
    private static final Logger logger = LogManager.getLogger();
    private ExecutorService pool;
    private ProtocolHandler protocolHandler;
    private  SegmentsCalculator segmentsCalculator;
    private   DownloadInformation downloadInformation;
    private  LinkedBlockingQueue<ResultMessage> queue;
    private ControllerStatus controllerStatus;

    public Controller( Configuration configuration, int maxConcurrentDownload, String url) {
        this.maxConcurrentDownload = maxConcurrentDownload;
        this.url = url;
        this.configuration = configuration;

        ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(url);
        if(protocolHandler == null)
        {
            //TODO handle more than one URL
            //TODO test this
            logger.error("Received url {} but no protocol handler defined for it", url);
            throw new IllegalArgumentException("no protocol handler found for url" + url);
        }

        //we create the pool
        segmentsCalculator = BasicSegmentCalculator.getInstance();

    }
    Map<String,List<Storage>> initStorage(List<Segment> segments)
    {
        if(segments.size() == 1)
        {
            Storage storage = StorageFactory.getStorage(segments.get(0).getSrcUrl(),configuration.getStorageConfiguration().getDownloadFolder(),
                    configuration.getStorageConfiguration().getOutputStreamBufferSize());
            if(storage == null)
            {
                return Collections.emptyMap();
            }
            else
            {
                return Collections.singletonMap(segments.get(0).getSrcUrl(), Collections.singletonList(storage));
            }
        }
        else {
            HashMap<String,List<Storage>> storageMap = new HashMap<>();
            List<String> failedUrl = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                if(!failedUrl.contains(segments.get(i).getSrcUrl()))
                {
                    Storage storage = StorageFactory.getStorage(segments.get(i).getSrcUrl(),configuration.getStorageConfiguration().getDownloadFolder(),
                            configuration.getStorageConfiguration().getOutputStreamBufferSize());
                    if(storage == null)
                    {
                        failedUrl.add(segments.get(i).getSrcUrl());
                    }else {
                        storageMap.compute(segments.get(i).getSrcUrl(),(s, storages) ->
                        {
                            if(storages == null)
                            {
                                return Collections.singletonList(storage);
                            }else {
                                storages.add(storage);
                                return storages;
                            }
                        });
                    }
                }
            }
            return storageMap;

        }
    }
    public void setup()
    {
        //fetch the info about the file
        downloadInformation = getFileInformation(maxConcurrentDownload);

        //create the download segments
        List<Segment> segmentList = createSegments();


        int effectiveConcurrency = Math.min(maxConcurrentDownload,segmentList.size());

        //create the queue
         queue = new LinkedBlockingQueue<>();

         //create the thread pool
        pool = Executors.newFixedThreadPool(effectiveConcurrency);

        //create the storage
        Map<String, List<Storage>> storages = initStorage(segmentList);


        //create the controller status
        Map<String, List<Segment>> segmentByUrl = segmentList.stream().collect(Collectors.groupingBy(Segment::getSrcUrl));

        ControllerStatus.Builder builder = new ControllerStatus.Builder().init(effectiveConcurrency);

        segmentByUrl.forEach((url, segments) ->
        {
            List<Storage> storageForUrl = storages.get(url);
            if(storageForUrl ==null  )
            {
                //i failed to create storage for this specifuc url to download: abort
                logger.info("Downlaod of the url {} will be cancelled",url);
            }else if(segments.size() != storages.size()){
                //this shoud never happened
                logger.fatal("This is quite unusual, i have storage not corresponding to the segments to download! Downlaod of the url {} will be cancelled",url);
            }else{
                for (int i = 0; i < segments.size(); i++) {
                    builder.add(segments.get(i),storageForUrl.get(i));
                }
            }
        });
        controllerStatus = builder.build();
    }


    public void run()
    {
        List<Segment> next = controllerStatus.getNext();
        if(next.size()>0)
        {
            //we create the runnable task
            for (Segment segment : next)
            {
                ProtocolHandler protocolHandler = ProtocolHandlerFactory.get(segment.getSrcUrl());
                //the protocol handler cannot be null because of the constructor check
                DownloaderRunnable runnable = new DownloaderRunnable(segment,protocolHandler,queue);
                Future<?> submit = pool.submit(runnable);
            }
            try {
                ResultMessage message = queue.take();
                handleMessage(message);
            } catch (InterruptedException e) {
                //something really bad happened
                //i can cleanup all unfinished download
                //TODO
            }
        }else
        //no segment to handle, either the concurrency threshold was met or im done

        {
            if(!controllerStatus.hasIdle())
            {
                //i'm done downloading! youpiii!! i should now join the piece if required

                //TODO
            }
        }


    }

    private void handleMessage(DownloaderMessage message)
    {
        if(DownloaderMessage.Type.RESULT == message.getType())
        {
            ResultMessage resultMessage = (ResultMessage)message;
            if (DownloadStatus.DOWNLOADING == resultMessage.getStatus() ||
                    DownloadStatus.FINISHED == resultMessage.getStatus())
            {
                Storage storage = controllerStatus.getStorage(message.getSegmentId());
                storage.push(resultMessage.getContent());

            }
            if (DownloadStatus.FINISHED == resultMessage.getStatus())
            {
                run();

            }
            else if(DownloadStatus.ERROR == resultMessage.getStatus())
            {
                //TODO cleanup
                // i should interrupt all donwload related to this URL
                //i should cleanup all storage related to this url
            }

        }



    }
    private List<Segment> createSegments()
    {
        return segmentsCalculator.getSegments(maxConcurrentDownload, configuration.getDownloaderConfiguration(), downloadInformation);
    }
    private DownloadInformation getFileInformation(int maxAttempts)
    {
        //we maintain the same logic and request this a few time until declaring failure
        int attempt = 1;

        DownloadInformation info =  new DownloadInformation(url,false,0);
        while ( attempt <= maxAttempts)
        {
            try {
                info = protocolHandler.getInfo(this.url);

            } catch (DownloadException e) {
                logger.error(String.format("Error while retrieving the information related to %s attempt %d max attempt allowed %d",url,attempt,maxAttempts),e);
                attempt++;
            }

        }
        return info;
    }


}
