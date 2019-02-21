package agoda.storage;

import agoda.storage.RandomAccessFileFileStorage;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class RandomAccessFileFileStorageTest {

    @Test
    public void testJoiningAfterWritingOnMultipleFiles()
    {

        File tempFile = null;
        int chunk = 1024;
        String url = "http://ipv4.download.thinkbroadband.com/5MB.txt";

        try {
            tempFile = getTempDirectory();
            List<RandomAccessFileFileStorage> storages = new ArrayList<>();
            final Random r = new Random();
            List<byte[]> expecteds = new ArrayList<>();
            for (int i = 0; i < 5; i++)
            {
                RandomAccessFileFileStorage rafs = new RandomAccessFileFileStorage(url,tempFile.getParentFile().getAbsolutePath());
                byte[] src = new byte[chunk];
                r.nextBytes(src);
                rafs.push(src);
                rafs.close();
                expecteds.add(src);
                storages.add(rafs);
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
            RandomAccessFileFileStorage[] rafsArray = new RandomAccessFileFileStorage[storages.size()];
            storages.toArray(rafsArray);
            RandomAccessFileFileStorage.join(rafsArray);

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
                storages.get(i).cleanUp();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }
    private File getTempDirectory() throws IOException {
        return Files.createTempDirectory("agoda").toFile();

    }
    @Test
    public void testMultiWritingWithMultipleThreadWritingMultipleFile()
    {
        String name = UUID.randomUUID().toString();
        String url = "http://ipv4.download.thinkbroadband.com/5MB.txt";
        File tempFile = null;
        final int  chunk = 1024;

        try {
            tempFile = getTempDirectory();
            List<RandomAccessFileFileStorage> storages = new ArrayList<>();
            for (int i = 0; i < 5; i++)
            {
                RandomAccessFileFileStorage randomAccessFileStorage = new RandomAccessFileFileStorage(url,tempFile.getParentFile().getAbsolutePath());
                storages.add(randomAccessFileStorage);
            }
            final Random r = new Random();
            List<Future<byte[]>> results = new ArrayList<>();

            final List<RandomAccessFileFileStorage> synchronizedList = Collections.synchronizedList(storages);
            final ExecutorService executorService = Executors.newFixedThreadPool(5);
            for (int i = 0; i < 5; i++) {
                final int index = i;
                Future<byte[]> submit = executorService.submit(new Callable<byte[]>() {

                    @Override
                    public byte[] call() throws IOException {

                        byte[] src = new byte[chunk];
                        r.nextBytes(src);
                        synchronizedList.get(index).push(src);
                        synchronizedList.get(index).close();
                        return src;

                    }

                });
                results.add(submit);
            }
            executorService.shutdown();
            executorService.awaitTermination(5,TimeUnit.SECONDS);

            //i should have 5 created files with random 1024 bytes inside
            for (int i = 0; i < 5; i++) {
                File file = new File(synchronizedList.get(i).fileName);
                RandomAccessFile raf = new RandomAccessFile(file,"r");
                byte[] expected = results.get(i).get();
                byte [] buffer = new byte[chunk];
                raf.read(buffer);
                assertArrayEquals(expected,buffer);
                raf.close();


            }


            for (int i = 0; i < synchronizedList.size(); i++) {
                synchronizedList.get(i).cleanUp();
            }



        }catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }

    }



    @Test
    public void testMultiWritingWithMultipleThreadWritingOnTheSameFile()
    {
        String url = "http://ipv4.download.thinkbroadband.com/5MB.txt";
        File tempFile = null;
        int chunk = 1024 * 1024;
        try {
            tempFile = getTempDirectory();
            List<RandomAccessFileFileStorage> storages = new ArrayList<>();
            for (int i = 0; i < 5; i++)
            {
                int offset = chunk * i;
                RandomAccessFileFileStorage randomAccessFileStorage = new RandomAccessFileFileStorage(url,tempFile.getParentFile().getAbsolutePath(),offset);
                storages.add(randomAccessFileStorage);
            }
            String txt = "the dog is in the house";

            final byte[] bytes = txt.concat(System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

            final List<RandomAccessFileFileStorage> synchronizedList = Collections.synchronizedList(storages);
            final ExecutorService executorService = Executors.newFixedThreadPool(5);
            for (int i = 0; i < 5; i++) {
                final int index = i;
                executorService.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            synchronizedList.get(index).push(bytes);
                            synchronizedList.get(index).push(bytes);
                            synchronizedList.get(index).close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            File saved = new File(synchronizedList.get(0).fileName);
            RandomAccessFile raf = new RandomAccessFile(saved,"r");

            for (int i = 0; i < 5; i++) {
                //we check the amount of data written by every thread
                byte[] buffer = new byte[bytes.length];

                raf.read(buffer);
                assertArrayEquals(buffer,bytes);

                //we read twice and verify the content is the same
                buffer = new byte[bytes.length];
                raf.read(buffer);
                assertArrayEquals(buffer,bytes);

                //we seek to the next block from the current position
                if(i<4)
                {
                    int position = chunk ;
                    raf.seek(position);
                }

            }
            raf.close();

            for (int i = 0; i < storages.size(); i++) {
                storages.get(i).close();
            }
            saved.delete();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }finally {
            FileUtils.deleteQuietly(tempFile);
        }

    }





}