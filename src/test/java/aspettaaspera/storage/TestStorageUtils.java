package aspettaaspera.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestStorageUtils {
    public static File getTempDirectory() {
        try {
            return Files.createTempDirectory("aspettaaspera").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
