package agoda.storage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.util.List;

public abstract class Storage {

    /**
     * The file copy buffer size (30 MB)  taken from apache common  {@link FileUtils}
     */
    private static final long FILE_COPY_BUFFER_SIZE = FileUtils.ONE_MB * 30;
    private static final Logger logger = LogManager.getLogger();
    protected String fileName;
    protected boolean isClosed;

    /**
     * Join a list of multiple {@link Storage} into a single file
     * based on {@link FileUtils#copyFile}
     *
     * @param storages the list of {@link Storage} we want to join
     * @throws IOException
     */
    public static void join(List<Storage> storages,boolean cleanAfter) throws IOException {
        if(storages.size()>0)
        {
            try (RandomAccessFile start = new RandomAccessFile(storages.get(0).fileName, "rw")) {
                for (int i = 1; i < storages.size(); i++) {

                    try (RandomAccessFile input = new RandomAccessFile(storages.get(i).fileName, "r")) {
                        long pos = start.length();
                        final long size = input.length() + pos;
                        long count;
                        while (pos < size) {
                            final long remain = size - pos;
                            count = remain > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : remain;
                            final long bytesCopied = start.getChannel().transferFrom(input.getChannel(), pos, count);
                            if (bytesCopied == 0) { // IO-385 - can happen if file is truncated after caching the size
                                break; // ensure we don't loop forever
                            }
                            pos += bytesCopied;
                        }
                    }
                    if(cleanAfter)
                    {
                        //i delete the file i just copied
                        FileUtils.deleteQuietly(new File(storages.get(i).fileName));

                    }

                }
            }
        }

    }

    public String getFileName() {
        return fileName;
    }

    /**
     * push some bytes to to storage
     *
     * @param buffer the byte array we want to write to the storage
     * @return true if the operation ended successfully, false otherwise
     */
    public abstract boolean push(byte[] buffer);

    /**
     * close the underlying access to the resource
     *
     * @throws IOException
     */
    public abstract void close() throws IOException;

    File getFile(String url, String destinationFolder, boolean increment) {
        String fileName = null;
        try {
            fileName = StorageUtils.getFileNameBasedOnUrl(url);
        } catch (MalformedURLException e) {
            logger.error("%s is not a valid URL, aborting", e);
            return null;
        }
        if (fileName == null || fileName.isEmpty()) {
            logger.error("Could not defer a name based on url {}, Aborting!", url);
            return null;
        }

        String destinationPath = FilenameUtils.concat(destinationFolder, fileName);
        File file = new File(destinationPath);
        int count = 1;
        while (file.exists() && count < 100 && increment) {
            //we add a suffix to the filename and increment it
            String incFileName = StorageUtils.getIncrementalFileName(count, destinationPath);
            file = new File(incFileName);
            count++;
        }
        if (count == 100) {
            //we have more than 100 file with the same name, something is going wrong
            logger.warn("we have more than 100 file with the same name {} derived from url {}, something is going wrong", destinationPath, url);
            return null;
        }
        return file;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean reset() {
        logger.info("cleaning up: deleting file at {}", this.fileName);
        try {
            this.close();
            return new File(fileName).delete();
        } catch (IOException e) {
            logger.error("An exception occurred white cleaning up file at " + fileName, e);
            return false;
        }

    }
}
