package tukano.impl;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.impl.rest.TukanoRestServer;
import tukano.impl.storage.AzureBlobStorage;
import tukano.impl.storage.BlobStorage;
import utils.Hash;
import utils.Hex;
import utils.CookieCacheUtils;

import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.error;

public class JavaBlobs implements Blobs {

    private static Blobs instance;
    private static final Logger Log = Logger.getLogger(JavaBlobs.class.getName());

    public String baseURI;
    private final BlobStorage storage;
    private final CookieCacheUtils cookieCacheUtils = new CookieCacheUtils();

    synchronized public static Blobs getInstance() throws IOException {
        if (instance == null)
            instance = new JavaBlobs();
        return instance;
    }

    private JavaBlobs() {
        storage = AzureBlobStorage.getInstance();
        baseURI = String.format("%s/%s/", TukanoRestServer.serverURI, Blobs.NAME);
    }

    @Override
    public Result<Void> upload(String blobId, byte[] bytes, String token) {
        Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)), token));

        if (!isAuthenticated(blobId, token))
            return error(FORBIDDEN);

        return storage.write(toPath(blobId), bytes);
    }

    @Override
    public Result<byte[]> download(String blobId, String token) {
        Log.info(() -> format("download : blobId = %s, token=%s\n", blobId, token));

        if (!isAuthenticated(blobId, token))
            return error(FORBIDDEN);

        return storage.read(toPath(blobId));
    }

    @Override
    public Result<Void> delete(String blobId, String token) {
        Log.info(() -> format("delete : blobId = %s, token=%s\n", blobId, token));

        if (!isAuthenticated(blobId, token))
            return error(FORBIDDEN);

        return storage.delete(toPath(blobId));
    }

    @Override
    public Result<Void> deleteAllBlobs(String userId, String token) {
        Log.info(() -> format("deleteAllBlobs : userId = %s, token=%s\n", userId, token));

        if (!isAuthenticated(userId, token))
            return error(FORBIDDEN);

        return storage.delete(toPath(userId));
    }

    private boolean validBlobId(String blobId, String token) {
        return Token.isValid(token, blobId);
    }

    private String toPath(String blobId) {
        return blobId.replace("+", "/");
    }

    /**
     * Checks if the given token or cookie is valid for the specified user or blob.
     * @param identifier The user ID or blob ID to validate.
     * @param token The token to validate.
     * @return True if either token or cookie-based authentication is valid, false otherwise.
     */
    private boolean isAuthenticated(String identifier, String token) {
        // Check both token-based and cookie-based authentication
        boolean tokenValid = Token.isValid(token, identifier);
        boolean cookieValid = false;

        try {
            // Attempt cookie validation, catching any exceptions that may occur
            cookieValid = cookieCacheUtils.isCookieValid(identifier, token);
        } catch (Exception e) {
            Log.warning(() -> "Cookie cache validation failed, falling back to token-based authentication.");
        }

        // Authentication is successful if either token or cookie validation passes
        return tokenValid || cookieValid;
    }
}
