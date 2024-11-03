package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import utils.BinaryCacheUtils;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static utils.ResourceUtils.loadPropertiesFromResources;

public class AzureBlobStorage implements BlobStorage {
    private static final String propertiesFile = "azureblob.properties";
    private final BlobContainerClient containerClient;
    private static final Logger Log = Logger.getLogger(AzureBlobStorage.class.getName());

    private static AzureBlobStorage instance;
    private final BinaryCacheUtils cacheUtils = new BinaryCacheUtils();

    private static final String BLOB_CACHE_PREFIX = "blob:";

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

        // Update cache after write
        cacheUtils.storeInCache(BLOB_CACHE_PREFIX + path, bytes, 3600); // Set 1-hour TTL
        return Result.ok();
    }

    @Override
    public Result<Void> delete(String path) {
        var blob = containerClient.getBlobClient(path);

        // Attempt to delete from cache
        cacheUtils.deleteFromCache(BLOB_CACHE_PREFIX + path);

        // Proceed with actual blob deletion
        return blob.deleteIfExists() ? Result.ok() : Result.error(Result.ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<byte[]> read(String path) {
        String cacheKey = BLOB_CACHE_PREFIX + path;

        // Attempt to retrieve from cache
        BinaryCacheUtils.CacheResult<byte[]> cacheResult = cacheUtils.getFromCache(cacheKey);
        if (cacheResult.isCacheHit()) {
            Log.info(() -> "Cache hit for blob: " + path);
            return Result.ok(cacheResult.getData());
        }

        // Cache miss - proceed with blob storage read
        var blob = containerClient.getBlobClient(path);
        byte[] data = blob.downloadContent().toBytes();
        if (data != null) {
            cacheUtils.storeInCache(cacheKey, data, 3600); // Store in cache with 1-hour TTL
            return Result.ok(data);
        } else {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        String cacheKey = BLOB_CACHE_PREFIX + path;

        // Attempt to retrieve from cache
        BinaryCacheUtils.CacheResult<byte[]> cacheResult = cacheUtils.getFromCache(cacheKey);
        if (cacheResult.isCacheHit()) {
            Log.info(() -> "Cache hit for blob: " + path);
            sink.accept(cacheResult.getData());
            return Result.ok();
        }

        // Cache miss - proceed with blob storage read
        var blob = containerClient.getBlobClient(path);
        byte[] data = blob.downloadContent().toBytes();
        if (data != null) {
            cacheUtils.storeInCache(cacheKey, data, 3600); // Store in cache with 1-hour TTL
            sink.accept(data);
            return Result.ok();
        } else {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}
