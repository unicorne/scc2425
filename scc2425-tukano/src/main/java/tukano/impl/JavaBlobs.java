package tukano.impl;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.impl.rest.RestShortsResource;
import tukano.impl.rest.TukanoRestServer;
import tukano.impl.storage.AzureBlobStorage;
import tukano.impl.storage.BlobStorage;
import utils.AuthUtils;
import utils.Hash;
import utils.Hex;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

public class JavaBlobs implements Blobs {

    private static Blobs instance;
    private static final Logger Log = Logger.getLogger(JavaBlobs.class.getName());

    public String baseURI;
    private final BlobStorage storage;

    synchronized public static Blobs getInstance() {
        if (instance == null)
            instance = new JavaBlobs();
        return instance;
    }

    private JavaBlobs() {
        storage = AzureBlobStorage.getInstance();
        baseURI = String.format("%s/%s/", TukanoRestServer.serverURI, Blobs.NAME);
    }

    @Override
    public Result<Void> upload(String blobId, byte[] bytes, Cookie cookie) {
        Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)), cookie));

        try {
            AuthUtils.validateSession(cookie);
        } catch (NotAuthorizedException e){
            return Result.error(Result.ErrorCode.UNAUTHORIZED);
        }

        return storage.write(toPath(blobId), bytes);
    }

    @Override
    public Result<byte[]> download(String blobId, Cookie cookie) {
        Log.info(() -> format("download : blobId = %s, token=%s\n", blobId, cookie));

        try {
            AuthUtils.validateSession(cookie);
        } catch (NotAuthorizedException e){
            return Result.error(Result.ErrorCode.UNAUTHORIZED);
        }

        return storage.read(toPath(blobId));
    }

    @Override
    public Result<Void> delete(String blobId, Cookie cookie) {
        Log.info(() -> format("delete : blobId = %s, token=%s\n", blobId, cookie));

        try {
            AuthUtils.validateSession(cookie);
        } catch (NotAuthorizedException e){
            return Result.error(Result.ErrorCode.UNAUTHORIZED);
        }

        return storage.delete(toPath(blobId));
    }

    @Override
    public Result<Void> deleteAllBlobs(String userId, Cookie cookie) {
        Log.info(() -> format("deleteAllBlobs : userId = %s, token=%s\n", userId, cookie));

        try {
            AuthUtils.validateSession(cookie);
        } catch (NotAuthorizedException e){
            return Result.error(Result.ErrorCode.UNAUTHORIZED);
        }

        List<String> shorts = new RestShortsResource().getShorts(userId);
        for (String shortId : shorts) {
            Result<Void> result = storage.delete(toPath(shortId));
            if (!result.isOK())
                return result;
        }
        return Result.ok();
    }

    private String toPath(String blobId) {
        return blobId.replace("+", "/");
    }
}
