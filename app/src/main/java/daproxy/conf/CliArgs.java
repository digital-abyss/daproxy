package daproxy.conf;

import com.beust.jcommander.Parameter;

import lombok.Getter;

public class CliArgs {
    
    @Parameter(names = "-config", description =  "Path to configuration file" )
    @Getter
    private String pathToConfigFile;
}
