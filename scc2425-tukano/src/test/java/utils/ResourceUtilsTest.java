package utils;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceUtilsTest {

    @Test
    void testLoadPropertiesFromResources() {
        Properties props = new Properties();
        ResourceUtils.loadPropertiesFromResources(props, "azureblobstest.properties");
        assertEquals("abc", props.getProperty("storageConnectionString"));
    }
}
