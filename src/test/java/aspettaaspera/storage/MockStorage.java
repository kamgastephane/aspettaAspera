package aspettaaspera.storage;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;

public class MockStorage extends Storage{
    private ByteArrayOutputStream buffer ;

    public MockStorage(int size) {
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public boolean push(byte[] buffer) {
        try {
            this.buffer.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean reset() {
        try {
            close();
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        this.isClosed = true;
    }
    public ByteArrayOutputStream getbuffer()
    {
        return  buffer;
    }
}
