package agoda.downloader;

import agoda.configuration.Configuration;
import agoda.configuration.DownloaderConfiguration;
import agoda.configuration.StorageConfiguration;
import agoda.storage.Storage;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ControllerTest {




    @Test
    public void generateStorageUsingSegmentWithBothGoodAndBadUrlsAndVerifyTheStorageIsOnlyForTheGoodUrls() {
        File file = null;
        try {

             file = Files.createTempDirectory("agoda").toFile();

            String goodUrl1 = "http://www.agoda.com";
            String badUrl1 = "qwertyuioplkjjhgfdsa";
            String goodUrl2 = "http://www.booking.com";

            Segment segment1 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(badUrl1).createSegment();
            Segment segment2 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(badUrl1).createSegment();
            Segment segment3 = new Segment.SegmentBuilder().setSegmentIndex(3).setSrcUrl(badUrl1).createSegment();
            Segment segment4 = new Segment.SegmentBuilder().setSegmentIndex(4).setSrcUrl(badUrl1).createSegment();

            Segment segment5 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(goodUrl1).createSegment();
            Segment segment6 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(goodUrl1).createSegment();
            Segment segment7 = new Segment.SegmentBuilder().setSegmentIndex(3).setSrcUrl(goodUrl1).createSegment();
            Segment segment8 = new Segment.SegmentBuilder().setSegmentIndex(4).setSrcUrl(goodUrl1).createSegment();


            Segment segment10 = new Segment.SegmentBuilder().setSegmentIndex(1).setSrcUrl(goodUrl2).createSegment();
            Segment segment11 = new Segment.SegmentBuilder().setSegmentIndex(2).setSrcUrl(goodUrl2).createSegment();

            //i should have storage created only for the two goods urls
            File finalFile = file;
            Configuration config = new Configuration() {
                @Override
                public DownloaderConfiguration getDownloaderConfiguration() {
                    return null;
                }

                @Override
                public StorageConfiguration getStorageConfiguration() {
                    return new StorageConfiguration() {
                        @Override
                        public int getOutputStreamBufferSize() {
                            return 64;
                        }

                        @Override
                        public String getDownloadFolder() {
                            return finalFile.getAbsolutePath();


                        }
                    };
                }
            };
            Controller controller = new Controller(config,10,goodUrl1);

            Map<String, List<Storage>> storage = controller.initStorage(Arrays.asList(segment1, segment2, segment3, segment4, segment5, segment6, segment7, segment8, segment10, segment11));
            assertEquals(storage.size(),2);
            assert storage.keySet().contains(goodUrl1);
            assert storage.keySet().contains(goodUrl2);
            assertEquals(storage.get(goodUrl1).size(),4);
            assertEquals(storage.get(goodUrl2).size(),2);


        }catch (IOException ignored) {
        }finally {
            if (file!=null)file.delete();
        }



        }

}