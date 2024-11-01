package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

public class AzureBlobStorage implements BlobStorage {
    private static final String propertiesFile = "src/main/resources/azureblob.properties";
    private final BlobContainerClient containerClient;

    public AzureBlobStorage(final String containerName) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        containerClient = new BlobContainerClientBuilder()
                .connectionString(props.getProperty("storageConnectionString"))
                .containerName(containerName)
                .buildClient();
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
        return blob.deleteIfExists()  ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
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
