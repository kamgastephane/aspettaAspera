package agoda.storage;

import agoda.storage.BufferedFileFileStorage;
import agoda.storage.StorageUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class BufferedFileFileStorageTest {

    @Test
    public void init() {
        File tempFile = null;
        try {

            tempFile =getTempDirectory();

            String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
            BufferedFileFileStorage storage = new BufferedFileFileStorage(url,tempFile.getAbsolutePath(), (int)FileUtils.ONE_MB);
            Assert.assertNotNull(storage);

            File f = new File(tempFile
                    ,"5MB.zip");
            boolean exists = f.exists();
            assert exists;

            boolean success = storage.cleanUp();
            assert success;

            exists = f.exists();
            assert !exists;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }

    }
    private File getTempDirectory() throws IOException {
        return Files.createTempDirectory("agoda").toFile();

    }

    @Test
    public void testWriting() {
        File tempFile = null;

        try {
            tempFile =getTempDirectory();

            String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
            BufferedFileFileStorage storage = new BufferedFileFileStorage(url,tempFile.getParentFile().getAbsolutePath(), (int)FileUtils.ONE_MB);
            Assert.assertNotNull(storage);
            String txt = "the dog is in the house";
            byte[] bytes = txt.concat(System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            int totalBytes = 0;
            while (totalBytes<1024*1024)
            {
                boolean push = storage.push(bytes);
                assertTrue(push);
                totalBytes+=bytes.length;
            }
            storage.close();
            File f = new File(tempFile.getParent(),"5MB.zip");
            List<String> result = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for(String s : result)
            {
                assertEquals(s, txt);
            }
            f.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }

    }

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