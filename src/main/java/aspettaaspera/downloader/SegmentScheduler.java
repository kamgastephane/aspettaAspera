package aspettaaspera.downloader;

import java.util.Collection;
import java.util.List;

public interface SegmentScheduler {

    /**
     * get the next segment to be fed to the {@link DownloaderRunnable} based on the concurrency allowed
     */
    List<Segment> getNext(Collection<Segment> segments, int concurrency);
}
