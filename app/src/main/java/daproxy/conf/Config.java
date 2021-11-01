package daproxy.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import lombok.Setter;

public class Config {

    @Getter
    private static Config config;

    @Getter @Setter
    private List<String> allowList;


    public static void loadConfig(String filename) throws FileNotFoundException, IOException{
        Yaml yaml = new Yaml();
        try (InputStream ios = new FileInputStream(new File(filename))) {
            config = yaml.loadAs(ios, Config.class);
        }
            
    }
}
