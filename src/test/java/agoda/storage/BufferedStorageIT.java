package agoda.storage;

import agoda.downloader.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class BufferedStorageIT {
    @Test
    public void init() {
        File tempFile = null;
        try {

            tempFile = TestStorageUtils.getTempDirectory();

            String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
            BufferedStorage storage = new BufferedStorage(url,tempFile.getAbsolutePath(), (int) FileUtils.ONE_MB);
            Assert.assertNotNull(storage);

            File f = new File(tempFile
                    ,"5MB.zip");
            boolean exists = f.exists();
            assert exists;

            boolean success = storage.reset();
            assert success;

            exists = f.exists();
            assert !exists;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }

    }

    @Test
    public void testJoiningAfterWritingOnMultipleFiles()
    {

        File tempFile = null;
        int chunk = 1024;
        String url = "http://ipv4.download.thinkbroadband.com/5MB.txt";

        try {
            tempFile = TestStorageUtils.getTempDirectory();
            List<Storage> storages = new ArrayList<>();
            final Random r = new Random();
            List<byte[]> expecteds = new ArrayList<>();
            for (int i = 0; i < 5; i++)
            {
                BufferedStorage bufferedStorage = new BufferedStorage(url,tempFile.getAbsolutePath(),chunk);
                byte[] src = new byte[chunk];
                r.nextBytes(src);
                bufferedStorage.push(src);
                bufferedStorage.close();
                expecteds.add(src);
                storages.add(bufferedStorage);
            }
            //i should have 5 created files with random 1024 bytes inside
            for (int i = 0; i < 5; i++) {
                File file = new File(storages.get(i).fileName);
                RandomAccessFile raf = new RandomAccessFile(file,"r");
                byte[] expectedBytes = expecteds.get(i);
                byte [] buffer = new byte[chunk];
                raf.read(buffer);
                assertArrayEquals(expectedBytes,buffer);
                raf.close();

            }
            storages.forEach(storage -> {
                try {
                    storage.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            BufferedStorage.join(storages,false);

            //we now have a single file which should contains all the bytes from the previous 4 other temp files
            File resultFile = new File(storages.get(0).fileName);
            RandomAccessFile raf = new RandomAccessFile(resultFile,"r");

            for (int i = 0; i < 5; i++) {
                //we check the amount of data written by every thread
                byte[] buffer = new byte[chunk];

                raf.read(buffer);
                assertArrayEquals(buffer,expecteds.get(i));


            }
            raf.close();

            for (int i = 0; i < storages.size(); i++) {
                storages.get(i).reset();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }
    @Test
    public void testWriting() {
        File tempFile = null;

        try {
            tempFile =TestStorageUtils.getTempDirectory();

            String url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
            BufferedStorage storage = new BufferedStorage(url,tempFile.getAbsolutePath(), (int)FileUtils.ONE_MB);
            Assert.assertNotNull(storage);
            String txt = "the dog is in the house";
            byte[] bytes = txt.concat(System.lineSeparator()).getBytes();
            int totalBytes = 0;
            while (totalBytes<1024*1024)
            {
                boolean push = storage.push(bytes);
                assertTrue(push);
                totalBytes+=bytes.length;
            }
            storage.close();
            File f = new File(tempFile,"5MB.zip");
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
}
