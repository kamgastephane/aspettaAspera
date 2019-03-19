package aspettaaspera.storage;

import org.apache.commons.io.FilenameUtils;

import java.net.MalformedURLException;
import java.net.URL;

public final class StorageUtils {
    private StorageUtils() {
    }


    static String getIncrementalFileName(int count, String filename) {
        String extension = FilenameUtils.getExtension(filename);
        String nakedFileName = FilenameUtils.removeExtension(filename);
        return nakedFileName + "_" + count + "." + extension;
    }

    static String getFileNameBasedOnUrl(String inputUrl) throws MalformedURLException {
        URL url = null;
        url = new URL(inputUrl);


        String filename = FilenameUtils.getName(url.getPath());
        if (filename == null || filename.isEmpty()) {
            filename = inputUrl.replaceAll("[^A-Za-z1-9]+", "_");
            if (filename.endsWith("_")) {
                filename = filename.substring(0, filename.length() - 1);
            }
        }
        return filename;

    }
}
