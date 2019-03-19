package aspettaaspera.storage;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * It keeps a single file and write byte on it based on the position of a pointer
 * Using more than one instance of this class, we can write on the same file at different location almost 'simultaneously'
 */
class RandomAccessStorage extends Storage {

    private static final Logger logger = LogManager.getLogger();

    private RandomAccessFile raf;
    private boolean isClosed;

    /**
     * @param url               the url the name of the file should be based on
     * @param destinationFolder the root folder for writing
     * @param offset            the offset we will start writing from inside the destination file
     * @throws IOException if no filename could be defined based on the url
     *                     Using this constructor the destination file will be overwritten if it exists already
     */
    RandomAccessStorage(String url, String destinationFolder, long offset) throws IOException {
        File file = getFile(url, destinationFolder, false);
        if (file != null) {

            this.fileName = file.getAbsolutePath();
            FileUtils.touch(file);
            this.raf = new RandomAccessFile(file, "rw");
            this.raf.seek(offset);

        } else {
            throw new IllegalArgumentException(String.format("An error occurred while creating a file using  inputs url %s and destination folder %s", url, destinationFolder));
        }
    }

    /**
     * @param url               the url the name of the file should be based on
     * @param destinationFolder the root folder for writing
     * @throws IOException if no filename could be defined based on the url
     *                     If the destination file already exists, the filename will be incremented
     */
    public RandomAccessStorage(String url, String destinationFolder) throws IOException {
        File file = getFile(url, destinationFolder, true);
        if (file != null) {

            this.fileName = file.getAbsolutePath();
            FileUtils.touch(file);
            this.raf = new RandomAccessFile(file, "rw");
        } else {
            throw new IllegalArgumentException(String.format("An error occurred while creating a file using  inputs url %s and destination folder %s", url, destinationFolder));
        }
    }


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
        if (raf != null && !isClosed) {
            isClosed = true;
            raf.close();

        }
    }

}
