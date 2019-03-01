package agoda.protocols;

import agoda.RandomDowloaderUtils;
import agoda.downloader.DownloadException;
import agoda.downloader.DownloadInformation;
import com.sun.javaws.exceptions.InvalidArgumentException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class FTPProtocolHandler implements ProtocolHandler {

    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_CHUNK_SIZE = (int) (FileUtils.ONE_MB * 2);

    public static final String FTP_SCHEME = "FTP";
    private FTPClient client = new FTPClient();


    @Override
    public String getScheme() {
        return FTP_SCHEME;
    }

    @Override
    public void download(String url, Map<String, String> parameters, ProgressListener consumer) throws DownloadException {
        String username = parameters.get("username");
        String password = parameters.get("password");

        Integer port = null;

        String portAsString = parameters.get("port");
        if(RandomDowloaderUtils.IsNullOrWhiteSpace(portAsString))
        {
            try
            {
                port = Integer.parseInt(portAsString);

            }catch (NumberFormatException e) {
                logger.warn("failed to convert the parameter received port {} to an integer",portAsString);
            }
        }
        if(RandomDowloaderUtils.IsNullOrWhiteSpace(username))
        {
            logger.warn("no user was provided to connect to  {} using FTPProtocolHandler",url);
        }
        if(RandomDowloaderUtils.IsNullOrWhiteSpace(password))
        {
            logger.warn("no password was provided to connect to  {} using FTPProtocolHandler",url);
        }
        download(url,port,username,password,consumer);

    }

    @Override
    public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
        throw new DownloadException("Not implemented for the FTPProtocolHandler",  new IllegalArgumentException());
    }

    @Override
    public void download(String url, ProgressListener consumer) throws DownloadException {
        download(url,null,null,null,consumer);
    }

    public void download(String url, Integer port, String username, String password, ProgressListener consumer) throws DownloadException {
        try {
            if(port != null)
            {
                client.connect(url, port);
            }else {
                client.connect(url);
            }

            // After connection attempt, you should check the reply code to verify
            // success.
            int reply = client.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                logger.error("FTP server refused connection at {}, Returning error",url);
                consumer.consume(null);
                return;
            }
            if(!RandomDowloaderUtils.IsNullOrWhiteSpace(username) && !RandomDowloaderUtils.IsNullOrWhiteSpace(password))
            {
                boolean login = client.login(username, password);
                if(!login)
                {
                    logger.error("FTP server denied the connection at {} with the provided credentials, Returning error",url);
                    client.logout();
                    consumer.consume(null);
                    return;
                }
            }

            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.enterLocalPassiveMode();
            client.setBufferSize(DEFAULT_CHUNK_SIZE);

            try (InputStream inputStream = client.retrieveFileStream(url)){
                byte[] bytesIn = new byte[DEFAULT_CHUNK_SIZE];

                while(inputStream.read(bytesIn) != -1) {
                    consumer.consume(bytesIn);
                }
            }
            boolean success = client.completePendingCommand();
            if (success) {
                logger.info("File at {} has been downloaded successfully", url);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(client.isConnected()) {
                try {
                    client.logout();
                    client.disconnect();
                } catch(IOException ioe) {
                    // do nothing
                }
            }
        }
    }



    @Override
    public DownloadInformation getInfo(String url) {
        return null;
    }
}
