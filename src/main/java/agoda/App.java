package agoda;

import agoda.configuration.Configuration;
import agoda.configuration.YamlConfiguration;
import agoda.downloader.Controller;
import agoda.downloader.SimpleSegmentCalculator;
import agoda.protocols.ProtocolHandlerFactory;
import agoda.storage.StorageFactoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;


@CommandLine.Command(name = "Rdload", mixinStandardHelpOptions = true, version = "1.0")
public class App implements Runnable
{

    @CommandLine.Option(required = true,names = { "-c", "--config" }, description = "the configuration file.")
    private File configurationFile;

    @CommandLine.Parameters(index = "0", arity = "1..*", paramLabel = "Urls", description = "urls(s) to process.")
    private String[] inputUrls;

    @CommandLine.Option(names = { "-v", "--verbose" }, description = "verbose logging, Helpful for troubleshooting.")
    private boolean verbose;


    public static void main( String[] args )
    {
        CommandLine.run(new App(), args);

    }

    private  YamlConfiguration parseConfig()
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(configurationFile, YamlConfiguration.class);

        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public void run() {
        YamlConfiguration configuration = parseConfig();
        if(configuration==null){
            System.err.println("Invalid argument found corresponding to the configuration!");
            System.exit(-1);
        }
        if (verbose)
        {
            Configurator.setRootLevel(Level.DEBUG);
        }
        for (String url : inputUrls)
        {
            handle(configuration,url);
        }
        System.exit(1);



    }
    private void handle(Configuration config, String url)
    {
        //TODO create specific options to allow some additional parameters; e.g. the FTP
        Controller controller = new Controller(url,new HashMap<>(),config,
                config.getDownloaderConfiguration().getMaxConcurrency(),
                SimpleSegmentCalculator.getInstance(), ProtocolHandlerFactory.get(url), StorageFactoryImpl.getInstance()
                );
        controller.setup();
        controller.run();
    }
}
