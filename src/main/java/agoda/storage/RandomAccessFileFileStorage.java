package agoda.storage;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * It keeps a single file and write byte on it based on the position of a pointer
 * Using more than one instance of this class, we can write on the same file at different location almost 'simultaneously'
 */
public class RandomAccessFileFileStorage extends FileStorage {

    private static final Logger logger = LogManager.getLogger();
    private RandomAccessFile raf;
    private boolean isClosed;
    /**
     * The file copy buffer size (30 MB)  taken from apache common  {@link FileUtils}
     */
    private static final long FILE_COPY_BUFFER_SIZE = FileUtils.ONE_MB * 30;
    /**
     * @param url the url the name of the file should be based on
     * @param destinationFolder the root folder for writing
     * @param offset the offset we will start writing from inside the destination file
     * @throws IOException if no filename could be defined based on the url
     * Using this constructor the destination file will be overwritten if it exists already
     */
    public RandomAccessFileFileStorage(String url, String destinationFolder, long offset) throws IOException {
        // if the offset is more than
        File file = getFile(url, destinationFolder,false);
        if (file != null) {

            this.fileName = file.getAbsolutePath();
            FileUtils.touch(file);
            this.raf = new RandomAccessFile(file,"rw");
            this.raf.seek(offset);

        } else {
            throw new IllegalArgumentException(String.format("An error occurred while creating a file using  inputs url %s and destination folder %s", url, destinationFolder));
        }
    }
    /**
     * @param url the url the name of the file should be based on
     * @param destinationFolder the root folder for writing
     * @throws IOException if no filename could be defined based on the url
     * If the destination file already exists, the filename will be incremented
     */
    public RandomAccessFileFileStorage(String url, String destinationFolder) throws IOException {
        File file = getFile(url, destinationFolder,true);
        if (file != null) {

            this.fileName = file.getAbsolutePath();
            FileUtils.touch(file);
            this.raf = new RandomAccessFile(file,"rw");
        } else {
            throw new IllegalArgumentException(String.format("An error occurred while creating a file using  inputs url %s and destination folder %s", url, destinationFolder));
        }
    }

    /**
     * push some bytes to to storage
     * @param buffer the byte array we want to write to the storage
     * @return true if the operation ended successfully, false otherwise
     */
    @Override
    public boolean push(byte[] buffer) {
        try {
            raf.write(buffer);
            return true;
        } catch (IOException e) {
            logger.error("an error occured while writing to the random access file at " + fileName, e);
            return false;
        }
    }

    public void close() throws IOException {
        if (raf != null && !isClosed ) {
            isClosed = true;
            raf.close();

        }
    }

    /**
     * Join a list of multiple {@link RandomAccessFileFileStorage} into a single file
     * based on {@link FileUtils#copyFile}
     * @param rafs the list of {@link RandomAccessFileFileStorage} we want to join
     *
     * @throws IOException
     */
    static void join(RandomAccessFileFileStorage...rafs) throws IOException
    {
        RandomAccessFile start = new RandomAccessFile(rafs[0].fileName,"rw");
        for (int i = 1; i < rafs.length; i++) {

            RandomAccessFile input = new RandomAccessFile(rafs[i].fileName,"r");
            long pos = start.length();
            final long size = input.length() + pos;
            long count = 0;
            while (pos < size) {
                final long remain = size - pos;
                count = remain > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : remain;
                final long bytesCopied = start.getChannel().transferFrom(input.getChannel(), pos, count);
                if (bytesCopied == 0) { // IO-385 - can happen if file is truncated after caching the size
                    break; // ensure we don't loop forever
                }
                pos += bytesCopied;
            }
           input.close();
        }
        start.close();

    }

}
