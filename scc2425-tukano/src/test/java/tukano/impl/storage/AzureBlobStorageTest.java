package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageTest {

    @Mock
    BlobContainerClient containerClientMock;

    @Mock
    BlobClient blobClientMock;

    @Captor
    ArgumentCaptor<BinaryData> binaryDataCaptor;

    AzureBlobStorage azureBlobStorage;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(AzureBlobStorageTest.class);
        azureBlobStorage = new AzureBlobStorage(containerClientMock);
    }

    @Test
    void testWrite() {
        String blobId = "blobId";
        byte[] data = "data".getBytes();
        when(containerClientMock.getBlobClient(blobId)).thenReturn(blobClientMock);

        assertTrue(azureBlobStorage.write(blobId, data).isOK());
        verify(blobClientMock).upload(binaryDataCaptor.capture());
        verify(containerClientMock).getBlobClient(blobId);
        assertArrayEquals(data, binaryDataCaptor.getValue().toBytes());
    }

    @Test
    void testRead_returningBytes() {
        // TODO: implement
    }

    @Test
    void testRead_consumingBytes() {
        // TODO: implement
    }

    @Test
    void testDelete() {
        // TODO: implement
    }
}
