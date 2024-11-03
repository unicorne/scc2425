package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;

import java.util.Properties;
import java.util.function.Consumer;

import static utils.ResourceUtils.loadPropertiesFromResources;

public class AzureBlobStorage implements BlobStorage {
    private static final String propertiesFile = "azureblob.properties";
    private final BlobContainerClient containerClient;

    private static AzureBlobStorage instance;

    private AzureBlobStorage() {
        Properties props = new Properties();
        loadPropertiesFromResources(props, propertiesFile);
        containerClient = new BlobContainerClientBuilder()
                .connectionString(props.getProperty("storageConnectionString"))
                .containerName(props.getProperty("blobContainerName"))
                .buildClient();
    }

    public static AzureBlobStorage getInstance() {
        if (instance == null)
            instance = new AzureBlobStorage();
        return instance;
    }

    /**
     * use this constructor only for test purposes to test interaction with the container client
     */
    public AzureBlobStorage(final BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        var blob = containerClient.getBlobClient(path);
        var data = BinaryData.fromBytes(bytes);
        blob.upload(data);
        return Result.ok();
    }

    @Override
    public Result<Void> delete(String path) {
        var blob = containerClient.getBlobClient(path);
        return blob.deleteIfExists() ? Result.ok() : Result.error(Result.ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<byte[]> read(String path) {
        var blob = containerClient.getBlobClient(path);
        byte[] data = blob.downloadContent().toBytes();
        return data != null ? Result.ok(data) : Result.error(Result.ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        var blob = containerClient.getBlobClient(path);
        byte[] data = blob.downloadContent().toBytes();
        if (data != null) {
            sink.accept(data);
            return Result.ok();
        } else {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}
