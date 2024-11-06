package tukano.impl;

import org.junit.jupiter.api.*;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import utils.Hash;
import utils.Hex;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

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
        String validToken = Token.get(blobId);

        Result<Void> result = blobs.upload(blobId, TEST_BYTES, validToken);

        assertTrue(result.isOK(), "Upload should succeed with valid token");
    }

    @Test
    void testUploadWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        String invalidToken = "invalid-token";

        Result<Void> result = blobs.upload(blobId, TEST_BYTES, invalidToken);

        assertFalse(result.isOK());
        assertEquals(FORBIDDEN, result.error());
    }

    @Test
    void testDownloadSuccess() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        String validToken = Token.get(blobId);
        blobs.upload(blobId, TEST_BYTES, validToken);

        Result<byte[]> result = blobs.download(blobId, validToken);

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
        String validToken = Token.get(blobId);
        blobs.upload(blobId, TEST_BYTES, validToken);
        String invalidToken = "invalid-token";

        Result<byte[]> result = blobs.download(blobId, invalidToken);

        assertFalse(result.isOK());
        assertEquals(FORBIDDEN, result.error());
    }

    @Test
    void testDeleteSuccess() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        String validToken = Token.get(blobId);
        blobs.upload(blobId, TEST_BYTES, validToken);

        Result<Void> deleteResult = blobs.delete(blobId, validToken);

        assertTrue(deleteResult.isOK(), "Delete should succeed with valid token");

        // Verify blob is actually deleted
        Result<byte[]> downloadResult = blobs.download(blobId, validToken);
        assertFalse(downloadResult.isOK(), "Blob should not exist after deletion");
    }

    @Test
    void testDeleteWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        String validToken = Token.get(blobId);
        blobs.upload(blobId, TEST_BYTES, validToken);
        String invalidToken = "invalid-token";

        Result<Void> result = blobs.delete(blobId, invalidToken);

        assertFalse(result.isOK());
        assertEquals(FORBIDDEN, result.error());
    }

    @Test
    void testDeleteAllBlobsSuccess() {
        String password = "pwd";
        User testUser = new User(userId, password, "testuser@mail.org", "Test User");
        assertTrue(AzureUsers.getInstance().createUser(testUser).isOK());

        Result<Short> short1res = AzureShorts.getInstance().createShort(userId, password);
        assertTrue(short1res.isOK());
        Result<Short> short2res = AzureShorts.getInstance().createShort(userId, password);
        assertTrue(short2res.isOK());

        String blobId1 = short1res.value().getId();
        String validToken1 = Token.get(blobId1);
        String blobId2 = short2res.value().getId();
        String validToken2 = Token.get(blobId2);
        blobs.upload(blobId1, TEST_BYTES, validToken1);
        blobs.upload(blobId2, TEST_BYTES, validToken2);

        Result<Void> result = blobs.deleteAllBlobs(userId, Token.get(userId));

        assertTrue(result.isOK(), "DeleteAllBlobs should succeed with valid token");

        // Verify blobs are actually deleted
        assertFalse(blobs.download(blobId1, validToken1).isOK(), "Blob 1 should not exist");
        assertFalse(blobs.download(blobId2, validToken2).isOK(), "Blob 2 should not exist");
    }

    @Test
    void testDeleteAllBlobsWithInvalidToken() {
        String blobId = userId + "/test-blob-" + UUID.randomUUID();
        String validToken = Token.get(blobId);
        blobs.upload(blobId, TEST_BYTES, validToken);
        String invalidToken = "invalid-token";

        Result<Void> result = blobs.deleteAllBlobs(userId, invalidToken);

        assertFalse(result.isOK());
        assertEquals(FORBIDDEN, result.error());
    }

    @AfterAll
    static void tearDown() {
        blobs.deleteAllBlobs(userId, Token.get(userId));
    }
}