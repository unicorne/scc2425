package utils;

import java.io.IOException;
import java.util.Properties;

public class ResourceUtils {

    public static void loadPropertiesFromResources(final Properties props, final String resourceFile){
        try {
            props.load(ResourceUtils.class.getClassLoader().getResourceAsStream(resourceFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
