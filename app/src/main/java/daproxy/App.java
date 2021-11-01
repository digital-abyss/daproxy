package daproxy;

import com.beust.jcommander.JCommander;

import daproxy.conf.CliArgs;
import daproxy.conf.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public String getGreeting() {
        return "Starting DA.Proxy";
    }

    public static void main(String[] args) {
        CliArgs parsedArgs = new CliArgs();
        JCommander.newBuilder().addObject(parsedArgs).build().parse(args);

        log.info(new App().getGreeting());
        log.debug("Path to config file = {} ", parsedArgs.getPathToConfigFile() );

        try {
            Config.loadConfig(parsedArgs.getPathToConfigFile());
            new Server().start();
        } catch (Exception ex) {
            log.error("Error booting server", ex);
        }

    }
}
