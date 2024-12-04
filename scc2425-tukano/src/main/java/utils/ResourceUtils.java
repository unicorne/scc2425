package utils;

import java.io.IOException;
import java.util.Properties;

public class ResourceUtils {

    public static void loadPropertiesFromResources(final Properties props, final String resourceFile) {
        try (var is = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceFile)) {
            if (is == null) {
                throw new IOException("Resource file not found: " + resourceFile);
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadResourceAsString(final String resourceFile) {
        try (var is = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceFile)) {
            if (is == null) {
                throw new IOException("Resource file not found: " + resourceFile);
            }
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
