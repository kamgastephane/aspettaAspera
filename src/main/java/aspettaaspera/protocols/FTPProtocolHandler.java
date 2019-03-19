package aspettaaspera.protocols;

import aspettaaspera.RandomDowloaderUtils;
import aspettaaspera.downloader.DownloadException;
import aspettaaspera.downloader.ResourceInformation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class FTPProtocolHandler implements ProtocolHandler {

    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_CHUNK_SIZE = (int) (FileUtils.ONE_MB * 2);
    private static final String FTP_SIZE_FEAT = "SIZE";

    static final String FTP_SCHEME = "ftp";
    private FTPClient client;


    @Override
    public String getScheme() {
        return FTP_SCHEME;
    }
    private static class FTPParameter{

        String username;
        String password;
        Integer port;
        Long from;
    }
    private FTPParameter parse( Map<String, String> parameters)
    {
        FTPParameter result = new FTPParameter();
        result.username = parameters.get("username");
        result.password = parameters.get("password");

        String portAsString = parameters.get("port");
        if(!RandomDowloaderUtils.IsNullOrWhiteSpace(portAsString))
        {
            try
            {
                result.port = Integer.parseInt(portAsString);

            }catch (NumberFormatException e) {
                logger.warn("failed to convert the parameter received port {} to an integer",portAsString);
            }
        }
        return result;
    }
    @Override
    public void download(String url, Map<String, String> parameters, ProgressListener consumer) throws DownloadException {
        FTPParameter connectionParams = parse(parameters);
        if(RandomDowloaderUtils.IsNullOrWhiteSpace(connectionParams.username))
        {
            logger.warn("no user was provided to connect to  {} using FTPProtocolHandler",url);
        }
        if(RandomDowloaderUtils.IsNullOrWhiteSpace(connectionParams.username))
        {
            logger.warn("no password was provided to connect to  {} using FTPProtocolHandler",url);
        }
        internalDownload(url,connectionParams,consumer);

    }
    @Override
    public void download(String url, long from, long to, ProgressListener consumer) throws DownloadException {
        FTPParameter connectionParams = new FTPParameter();
        connectionParams.from = from;
        internalDownload(url,connectionParams,consumer);


    }

    @Override
    public void download(String url, ProgressListener consumer) throws DownloadException {
        FTPParameter connectionParams = new FTPParameter();

        internalDownload(url,connectionParams,consumer);
    }

    private void internalDownload(String url, FTPParameter connectionParams, ProgressListener consumer) throws DownloadException {
        try {

            URI uri = new URI(url);
            boolean connectionResult = connect(uri, connectionParams);

            if (!connectionResult)
            {
                //THe connection attempt failed
                consumer.consume(null);
                return;
            }
            //TODO find a way to cut down the retry count of the downloader runnable as i will try n time to
            // connect with possibly the same failure happening if the credentials are not correct
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.enterLocalPassiveMode();
            client.setBufferSize(DEFAULT_CHUNK_SIZE);
            if(connectionParams.from != null)
            {
                client.setRestartOffset(connectionParams.from);
            }

            try (InputStream inputStream = client.retrieveFileStream(uri.getPath())){
                byte[] bytesIn = new byte[DEFAULT_CHUNK_SIZE];

                Instant now = Instant.now();
                int length;

                while((length = inputStream.read(bytesIn)) != -1) {
                    byte[]cpy = new byte[length];
                    System.arraycopy(bytesIn,0,cpy,0,length);

                    consumer.consume(cpy);

                    //we send a noop operation every 5 min to avoid the closing of the connection
                    if(elapsed(now,Duration.ofMinutes(5)))
                    {
                        client.sendNoOp();
                        now = Instant.now();
                    }


                }
            }
            boolean success = client.completePendingCommand();
            if (success) {
                logger.debug("Segment at {} has been downloaded successfully", url);
            }
            consumer.consume(EOF);

        }
        catch (IOException | URISyntaxException e) {
            throw new DownloadException("IO exception",e);
        }finally {

        }
    }


    private boolean elapsed(Instant start,Duration interval)
    {
        Instant then = Instant.now();
        return interval.minus(Duration.between(start,then)).isNegative();
    }

    private boolean connect(URI uri, FTPParameter connectionParams) throws IOException {
        if(client!= null && client.isConnected())return true;
        client = new FTPClient();
        if(connectionParams.port != null)
            {
                client.connect(uri.getHost(), connectionParams.port);
            }else {
                client.connect(uri.getHost());
            }
            int reply = client.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                logger.error("FTP server refused connection at {}, Returning error",uri.toString());
                return false;
            }
            if(RandomDowloaderUtils.IsNullOrWhiteSpace(connectionParams.username)){
                connectionParams.username = "anonymous";
            }
            if(RandomDowloaderUtils.IsNullOrWhiteSpace(connectionParams.password)){
                connectionParams.password = Thread.currentThread().getName();

            }

            boolean login = client.login(connectionParams.username, connectionParams.password);
            if(!login)
            {
                logger.error("FTP server denied the connection at {} with the provided credentials, Returning error",uri.toString());
                client.logout();
                return false;
            }
            return true;
    }

    @Override
    public ResourceInformation getInfo(String url, Map<String,String> parameters) throws DownloadException {
        client = new FTPClient();
        FTPParameter connectionParameters = parse(parameters);
        boolean acceptRestartFeature = false;
        long size = ResourceInformation.DEFAULT_SIZE;
        boolean connected;
        URI uri;
        try {
             uri = new URI(url);
            connected = connect(uri, connectionParameters);
        } catch (IOException e) {
            throw new DownloadException("IO exception while fetching info related to "+url,e);
        } catch (URISyntaxException e) {
            throw new DownloadException("Failed to convert url into URI ",e);
        }

        if(!connected)
        {
            return null;
        }
        //TODO verify  if the acceptRestart Feature with a real world server can be used for multithreading
//        try {
//            acceptRestartFeature = client.hasFeature(FTPCmd.RESTART.getCommand());
//        } catch (IOException e) {
//            //i handle this exception silently
//            logger.error("IO Exception while asking for the REST feature to the FTP server at "+url,e);
//        }


        try {
                //i set the file type as it may affect the response of the size command
                client.setFileType(FTP.BINARY_FILE_TYPE);
            String[] replies = client.doCommandAsStrings(FTP_SIZE_FEAT, uri.getPath());
            if(replies != null && replies.length > 0)
            {
                String reply = replies[0];
                //the first three char should be the response code, followed by a space and the the result
                logger.debug("Received {} while asking the size of the resource",reply);
                if(reply.length() > 4)
                {
                    String sizeAsString = reply.substring(4);
                    try {
                        size = Long.parseLong(sizeAsString);

                    }catch (NumberFormatException ex)
                    {
                        logger.error("NumberFormatException Exception while parsing the size of the file! we received the value "+ sizeAsString ,ex);
                    }

                }
            }
        } catch (IOException e) {
            logger.error("IO Exception while asking for the SIZE feature to the FTP server at "+url,e);
        }finally {
            cleanup();
        }
        return new ResourceInformation(url,false,size);
    }

    private void cleanup()
    {
        if(client!= null && client.isConnected()) {
            try {
                client.logout();
                client.disconnect();
                client = null;
            } catch(IOException ioe) {
                // do nothing
            }
        }
    }
    @Override
    public void close() {
       cleanup();
    }
}
