package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ResourceUtils {

    public static void loadPropertiesFromResources(final Properties props, final String resourceFile){
        try {
            props.load(ResourceUtils.class.getClassLoader().getResourceAsStream(resourceFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadResourceAsString(final String resourceFile) {
        try(InputStream is = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceFile)) {
            assert is != null;
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
