package utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilsTest {

    /**
     * for this test to work place a file 'test.properties' in src/test/resources and put the following content in it:
     * testKey=testValue
     */
    @Test
    void testLoadPropertiesFromResources() {
        Properties props = new Properties();

        assertDoesNotThrow(() ->
                ResourceUtils.loadPropertiesFromResources(props, "test.properties")
        );

        assertFalse(props.isEmpty(), "Properties should be loaded");
        assertEquals("testValue", props.getProperty("testKey"),
                "Specific property should be loaded correctly");
    }

    @Test
    void testLoadPropertiesFromNonExistentResource() {
        Properties props = new Properties();

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ResourceUtils.loadPropertiesFromResources(props, "non_existent.properties")
        );

        assertTrue(exception.getCause() instanceof IOException,
                "Exception should wrap an IOException");
    }

    @Test
    void testLoadResourceAsString() {
        String content = ResourceUtils.loadResourceAsString("test-content.txt");

        assertNotNull(content, "Resource content should not be null");
        assertFalse(content.isEmpty(), "Resource content should not be empty");
    }

    @Test
    void testLoadNonExistentResourceAsString() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ResourceUtils.loadResourceAsString("non_existent_resource.txt")
        );

        assertTrue(exception.getCause() instanceof IOException,
                "Exception should wrap an IOException");
    }

    @Test
    void testLoadEmptyResource() {
        String content = ResourceUtils.loadResourceAsString("empty-file.txt");

        assertNotNull(content, "Empty resource should return an empty string");
        assertTrue(content.isEmpty(), "Empty resource content should be an empty string");
    }
}
