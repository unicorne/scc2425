package tukano.impl;

import jakarta.ws.rs.core.NewCookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import tukano.impl.shorts.ShortsImpl;
import tukano.impl.users.UsersImpl;
import utils.Hash;
import utils.Hex;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.UNAUTHORIZED;
import static tukano.impl.rest.RestLoginResource.COOKIE_KEY;
import static tukano.impl.rest.RestLoginResource.MAX_COOKIE_AGE;
import static utils.AuthUtils.createCookie;

public class JavaBlobsTest {

    private static JavaBlobs blobs;
    private static String userId;
    private static final String TEST_CONTENT = "Hello, Blob Storage!";
    private static final byte[] TEST_BYTES = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void setUp() {
        blobs = (JavaBlobs) JavaBlobs.getInstance();
        userId = "test-user-" + UUID.randomUUID();
    }

    @Test
    void testUploadSuccess() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();

        Result<Void> result = blobs.upload(blobId, TEST_BYTES, createCookie(userId));

        assertTrue(result.isOK(), "Upload should succeed with valid token");
    }

    @Test
    void testUploadWithInvalidCookie() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();

        Result<Void> result = blobs.upload(blobId, TEST_BYTES, createUnknownCookie());

        assertFalse(result.isOK());
        assertEquals(UNAUTHORIZED, result.error());
    }

    @Test
    void testDownloadSuccess() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        var validCookie = createCookie(userId);
        blobs.upload(blobId, TEST_BYTES, validCookie);

        Result<byte[]> result = blobs.download(blobId, validCookie);

        assertTrue(result.isOK(), "Download should succeed with valid token");
        assertArrayEquals(TEST_BYTES, result.value());
        assertEquals(
                Hex.of(Hash.sha256(TEST_BYTES)),
                Hex.of(Hash.sha256(result.value())),
                "Content hash should match"
        );
    }

    @Test
    void testDownloadWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        var validCookie = createCookie(userId);
        blobs.upload(blobId, TEST_BYTES, validCookie);
        var invalidCookie = createUnknownCookie();

        Result<byte[]> result = blobs.download(blobId, invalidCookie);

        assertFalse(result.isOK());
        assertEquals(UNAUTHORIZED, result.error());
    }

    @Test
    void testDeleteSuccess() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        var validCookie = createCookie(userId);
        blobs.upload(blobId, TEST_BYTES, validCookie);

        Result<Void> deleteResult = blobs.delete(blobId, validCookie);

        assertTrue(deleteResult.isOK(), "Delete should succeed with valid token");

        // Verify blob is actually deleted
        Result<byte[]> downloadResult = blobs.download(blobId, validCookie);
        assertFalse(downloadResult.isOK(), "Blob should not exist after deletion");
    }

    @Test
    void testDeleteWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        var validCookie = createCookie(userId);
        blobs.upload(blobId, TEST_BYTES, validCookie);
        var invalidCookie = createUnknownCookie();

        Result<Void> result = blobs.delete(blobId, invalidCookie);

        assertFalse(result.isOK());
        assertEquals(UNAUTHORIZED, result.error());
    }

    @Test
    void testDeleteAllBlobsSuccess() {
        String password = "pwd";
        User testUser = new User(userId, password, "testuser@mail.org", "Test User");
        assertTrue(UsersImpl.getInstance().createUser(testUser).isOK());

        Result<Short> short1res = ShortsImpl.getInstance().createShort(userId, password);
        assertTrue(short1res.isOK());
        Result<Short> short2res = ShortsImpl.getInstance().createShort(userId, password);
        assertTrue(short2res.isOK());

        var validCookie = createCookie(userId);
        String blobId1 = short1res.value().getId();
        String blobId2 = short2res.value().getId();
        blobs.upload(blobId1, TEST_BYTES, validCookie);
        blobs.upload(blobId2, TEST_BYTES, validCookie);

        Result<Void> result = blobs.deleteAllBlobs(userId, validCookie);

        assertTrue(result.isOK(), "DeleteAllBlobs should succeed with valid token");

        // Verify blobs are actually deleted
        assertFalse(blobs.download(blobId1, validCookie).isOK(), "Blob 1 should not exist");
        assertFalse(blobs.download(blobId2, validCookie).isOK(), "Blob 2 should not exist");
    }

    @Test
    void testDeleteAllBlobsWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        var validCookie = createCookie(userId);
        blobs.upload(blobId, TEST_BYTES, validCookie);
        var invalidCookie = createUnknownCookie();

        Result<Void> result = blobs.deleteAllBlobs(userId, invalidCookie);

        assertFalse(result.isOK());
        assertEquals(UNAUTHORIZED, result.error());
    }

    @AfterAll
    static void tearDown() {
        blobs.deleteAllBlobs(userId, createCookie(userId));
    }

    private NewCookie createUnknownCookie() {
        String uuid = UUID.randomUUID().toString();
        return new NewCookie.Builder(COOKIE_KEY)
                .value(uuid).path("/")
                .comment("sessionid")
                .maxAge(MAX_COOKIE_AGE)
                .secure(false) //ideally it should be true to only work for https requests
                .httpOnly(true)
                .build();
    }
}