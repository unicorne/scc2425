package tukano.impl.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.junit.jupiter.api.*;
import tukano.api.Result;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static utils.ResourceUtils.loadPropertiesFromResources;

public class AzureBlobStorageTest {

    private static BlobStorage blobStorage;
    private static final String TEST_CONTAINER_PREFIX = "test-container-";
    private static BlobContainerClient containerClient;

    @BeforeAll
    static void setUp() {
        Properties props = new Properties();
        loadPropertiesFromResources(props, "azureblob.properties");

        // create a testcontainer that can be deleted later
        String testContainerName = TEST_CONTAINER_PREFIX + UUID.randomUUID();
        containerClient = new BlobContainerClientBuilder()
                .connectionString(props.getProperty("storageConnectionString"))
                .containerName(testContainerName)
                .buildClient();

        containerClient.create();
        blobStorage = new AzureBlobStorage(containerClient);
    }

    @AfterAll
    static void tearDown() {
        // Clean up test container
        if (containerClient != null) {
            containerClient.delete();
        }
    }

    @Test
    void testWriteAndReadBlob() {
        String blobPath = "test/sample.txt";
        byte[] testData = "Hello, Azure Blob Storage!".getBytes();

        Result<Void> writeResult = blobStorage.write(blobPath, testData);
        Result<byte[]> readResult = blobStorage.read(blobPath);

        assertTrue(writeResult.isOK(), "Write operation should succeed");
        assertTrue(readResult.isOK(), "Read operation should succeed");
        assertArrayEquals(testData, readResult.value(), "Read data should match written data");
    }

    @Test
    void testDeleteExistingBlob() {
        String blobPath = "test/to-delete.txt";
        byte[] testData = "This will be deleted".getBytes();
        blobStorage.write(blobPath, testData);

        Result<Void> deleteResult = blobStorage.delete(blobPath);

        assertTrue(deleteResult.isOK(), "Delete operation should succeed");
        Result<byte[]> readResult = blobStorage.read(blobPath);
        assertFalse(readResult.isOK(), "Blob should not exist after deletion");
    }

    @Test
    void testDeleteNonExistentBlob() {
        String nonExistentPath = "test/non-existent.txt";

        Result<Void> deleteResult = blobStorage.delete(nonExistentPath);

        assertFalse(deleteResult.isOK(), "Delete operation should fail for non-existent blob");
        assertEquals(Result.ErrorCode.NOT_FOUND, deleteResult.error(), "Error code should be NOT_FOUND");
    }

    @Test
    void testReadWithConsumer() {
        String blobPath = "test/consumer-test.txt";
        byte[] testData = "Testing consumer read".getBytes();
        blobStorage.write(blobPath, testData);
        AtomicReference<byte[]> consumedData = new AtomicReference<>();

        Result<Void> readResult = blobStorage.read(blobPath, consumedData::set);

        assertTrue(readResult.isOK(), "Read with consumer should succeed");
        assertArrayEquals(testData, consumedData.get(), "Consumed data should match original data");
    }

    @Test
    void testReadNonExistentBlob() {
        String nonExistentPath = "test/does-not-exist.txt";

        Result<byte[]> readResult = blobStorage.read(nonExistentPath);

        assertFalse(readResult.isOK(), "Read operation should fail for non-existent blob");
        assertEquals(Result.ErrorCode.NOT_FOUND, readResult.error(), "Error code should be INTERNAL_ERROR");
    }

    @Test
    void testLargeBlob() {
        String blobPath = "test/large-file.bin";
        byte[] largeData = new byte[5 * 1024 * 1024]; // 5MB test file
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        Result<Void> writeResult = blobStorage.write(blobPath, largeData);
        Result<byte[]> readResult = blobStorage.read(blobPath);

        assertTrue(writeResult.isOK(), "Write operation should succeed for large blob");
        assertTrue(readResult.isOK(), "Read operation should succeed for large blob");
        assertArrayEquals(largeData, readResult.value(), "Read data should match written data for large blob");
    }
}