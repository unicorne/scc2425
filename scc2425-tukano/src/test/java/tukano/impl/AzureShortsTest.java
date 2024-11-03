package tukano.impl;

import org.junit.jupiter.api.*;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static tukano.api.Result.ErrorCode.*;

public class AzureShortsTest {

    private static AzureShorts shorts;
    private static AzureUsers users;
    private static String testUserId1;
    private static String testUserId2;
    private User testUser1;
    private User testUser2;
    private static final String password = "testPassword123";
    private static String testShortId;

    @BeforeAll
    static void setup() {
        shorts = AzureShorts.getInstance();
        users = AzureUsers.getInstance();

        testUserId1 = "test-user-" + UUID.randomUUID();
        testUserId2 = "test-user-" + UUID.randomUUID();
    }

    @BeforeEach
    void createUsers(){
        testUser1 = new User(testUserId1, password, "testuser1@mail.org", "Test User 1");
        testUser2= new User(testUserId2, password, "testuser2@mail.org", "Test User 2");

        assertTrue(users.createUser(testUser1).isOK());
        assertTrue(users.createUser(testUser2).isOK());
    }

    @AfterAll
    static void cleanup() {
        shorts.deleteAllShorts(testUserId1, password, Token.get());
        shorts.deleteAllShorts(testUserId2, password, Token.get());
    }

    @AfterEach
    void deleteUsers(){
        users.deleteUser(testUserId1, testUser1.getPwd());
        users.deleteUser(testUserId2, testUser2.getPwd());
    }

    @Test
    void testCreateShort() {
        Result<Short> result = shorts.createShort(testUserId1, password);

        assertTrue(result.isOK());
        assertNotNull(result.value());
        assertEquals(testUserId1, result.value().getOwnerId());
        assertEquals(0, result.value().getTotalLikes());

        testShortId = result.value().getId();
    }

    @Test
    void testGetShort() {
        Result<Short> result = shorts.createShort(testUserId1, password);
        testShortId = result.value().getId();

        result = shorts.getShort(testShortId);

        assertTrue(result.isOK());
        assertNotNull(result.value());
        assertEquals(testShortId, result.value().getId());
        assertEquals(testUserId1, result.value().getOwnerId());
    }

    @Test
    void testGetShorts() {
        Result<Short> result = shorts.createShort(testUserId1, password);
        testShortId = result.value().getId();
        result = shorts.createShort(testUserId1, password);
        String testShortId2 = result.value().getId();

        Result<List<String>> listResult = shorts.getShorts(testUserId1);

        assertTrue(listResult.isOK());
        assertNotNull(listResult.value());
        assertTrue(listResult.value().contains(testShortId));
        assertTrue(listResult.value().contains(testShortId2));
    }

    @Test
    void testFollow() {
        Result<Void> followResult = shorts.follow(testUserId1, testUserId2, true, password);
        assertTrue(followResult.isOK());

        Result<List<String>> followersResult = shorts.followers(testUserId2, password);
        assertTrue(followersResult.isOK());
        assertTrue(followersResult.value().contains(testUserId1));

        // Test unfollow
        Result<Void> unfollowResult = shorts.follow(testUserId1, testUserId2, false, password);
        assertTrue(unfollowResult.isOK());

        followersResult = shorts.followers(testUserId2, password);
        assertFalse(followersResult.value().contains(testUserId1));
    }

    @Test
    void testLike() {
        Result<Short> result = shorts.createShort(testUserId1, password);
        testShortId = result.value().getId();

        Result<Void> likeResult = shorts.like(testShortId, testUserId2, true, password);
        assertTrue(likeResult.isOK());

        Result<Short> shortResult = shorts.getShort(testShortId);
        assertTrue(shortResult.isOK());
        assertEquals(1, shortResult.value().getTotalLikes());

        Result<List<String>> likesResult = shorts.likes(testShortId, password);
        assertTrue(likesResult.isOK());
        assertTrue(likesResult.value().contains(testUserId2));

        // Test unlike
        Result<Void> unlikeResult = shorts.like(testShortId, testUserId2, false, password);
        assertTrue(unlikeResult.isOK());

        shortResult = shorts.getShort(testShortId);
        assertEquals(0, shortResult.value().getTotalLikes());
    }

    @Test
    void testGetFeed() {
        // First follow testUserId2
        shorts.follow(testUserId1, testUserId2, true, password);

        // Create a short for testUserId2
        Result<Short> createShortResult = shorts.createShort(testUserId2, password);
        testShortId = createShortResult.value().getId();
        assertTrue(createShortResult.isOK());

        Result<List<String>> feedResult = shorts.getFeed(testUserId1, password);
        assertTrue(feedResult.isOK());
        assertTrue(feedResult.value().contains(testShortId));
        assertTrue(feedResult.value().contains(createShortResult.value().getId()));
    }

    @Test
    void testDeleteShort() {
        Result<Short> result = shorts.createShort(testUserId1, password);
        testShortId = result.value().getId();

        Result<Void> deleteResult = shorts.deleteShort(testShortId, password);
//        assertTrue(deleteResult.isOK());

        Result<Short> getResult = shorts.getShort(testShortId);
        assertEquals(NOT_FOUND, getResult.error());
    }

    @Test
    void testErrorCases() {
        // Test invalid password
        Result<Short> createWithBadPwd = shorts.createShort(testUserId1, "wrongpassword");
        assertFalse(createWithBadPwd.isOK());
        assertEquals(UNAUTHORIZED, createWithBadPwd.error());

        // Test non-existent short
        Result<Short> getNonExistent = shorts.getShort("non-existent-id");
        assertFalse(getNonExistent.isOK());
        assertEquals(NOT_FOUND, getNonExistent.error());

        // Test null shortId
        Result<Short> getNullShort = shorts.getShort(null);
        assertFalse(getNullShort.isOK());
        assertEquals(BAD_REQUEST, getNullShort.error());
    }

    @Test
    void testDeleteAllShorts() {
        // Create a few more shorts first
        shorts.createShort(testUserId1, password);
        shorts.createShort(testUserId1, password);

        Result<Void> deleteAllResult = shorts.deleteAllShorts(testUserId1, password, Token.get());
        assertTrue(deleteAllResult.isOK());

        Result<List<String>> getShortsResult = shorts.getShorts(testUserId1);
        assertTrue(getShortsResult.value().isEmpty());
    }
}