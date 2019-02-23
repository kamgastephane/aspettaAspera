package agoda.storage;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class BufferedFileStorage extends FileStorage {



    private static final Logger logger = LogManager.getLogger();
    private BufferedOutputStream outputStream;
    private boolean isClosed;

    BufferedFileStorage(String url, String destinationFolder, int bufferSize) throws IOException {

        File file = getFile(url, destinationFolder,true);
        if (file != null) {

            this.fileName = file.getAbsolutePath();
            FileUtils.touch(file);
            this.outputStream = new BufferedOutputStream(new FileOutputStream(file), bufferSize);

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
        //we should try to have close size between this byte array and the underlying bufferStream
        try {
            outputStream.write(buffer, 0, buffer.length);
            return true;
        } catch (IOException e) {
            logger.error("an error occured while writing to the stream at " + fileName, e);
            return false;
        }
    }

    /**
     * close the access to the underlying resource
     * @throws IOException
     */
    public void close() throws IOException {
        if (outputStream != null && !isClosed) {
            isClosed = true;
            outputStream.close();
        }
    }



}
