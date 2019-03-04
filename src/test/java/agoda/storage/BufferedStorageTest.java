package agoda.storage;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

public class BufferedStorageTest {



    @Test
    public void getFileNameBasedOnUrl() throws MalformedURLException {
        String url = "https://logging.apache.org/log4j/2.x/manual/api.html";
        String name = StorageUtils.getFileNameBasedOnUrl(url);

        Assert.assertNotNull(name);
        assertEquals("api.html", name);


        url = "https://logging.apache.org/log4j/2.x/manual";
        name = StorageUtils.getFileNameBasedOnUrl(url);
        Assert.assertNotNull(name);
        assertEquals("manual", name);


        url = "https://logging.apache.org/log4j/2.x/manual/";
        name = StorageUtils.getFileNameBasedOnUrl(url);
        Assert.assertNotNull(name);
        assertEquals("https_logging_apache_org_log4j_2_x_manual", name);

    }
}