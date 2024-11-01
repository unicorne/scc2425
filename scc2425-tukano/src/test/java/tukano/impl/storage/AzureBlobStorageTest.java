package tukano.impl.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AzureBlobStorageTest {

    @Test
    void testReadProperties() throws IOException {
        AzureBlobStorage azureBlobStorage = new AzureBlobStorage("something");
        assertNotNull(azureBlobStorage);
    }
}
