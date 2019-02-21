package agoda.storage;

import java.io.IOException;

public interface Storage  {
    boolean push(byte[] buffer);
    void close() throws IOException;
}
