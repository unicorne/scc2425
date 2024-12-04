package tukano.impl;

import org.junit.jupiter.api.*;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import tukano.impl.shorts.SQLShorts;
import tukano.impl.users.SQLUsers;
import testhelper.EnabledIfProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static utils.AuthUtils.createCookie;

@EnabledIfProperty(property = "dbtype", value = "postgresql", file = "db.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLShortsTest {
    private SQLShorts sqlShorts;
    private SQLUsers sqlUsers;
    private String testShortId;
    private static final String TEST_PASSWORD = "testPass123";
    private static final User TEST_USER_1 = new User("testUser1", TEST_PASSWORD, "testuser1@mail.org", "testuser1");
    ;
    private static final User TEST_USER_2 = new User("testUser2", TEST_PASSWORD, "testuser2@mail.org", "testuser2");
    ;

    @BeforeAll
    void setUp() {
        sqlUsers = SQLUsers.getInstance();
        sqlShorts = SQLShorts.getInstance();
        Result<String> user1Result = sqlUsers.createUser(TEST_USER_1);
        Result<String> user2Result = sqlUsers.createUser(TEST_USER_2);

        assertTrue(user1Result.isOK());
        assertTrue(user2Result.isOK());
    }

    @AfterAll
    void tearDown() {
        // Clean up test data
        sqlShorts.deleteAllShorts(TEST_USER_1.getId(), TEST_PASSWORD, createCookie(TEST_USER_1.getId()));
        sqlShorts.deleteAllShorts(TEST_USER_2.getId(), TEST_PASSWORD, createCookie(TEST_USER_1.getId()));
        sqlUsers.deleteUser(TEST_USER_1.getId(), TEST_PASSWORD);
        sqlUsers.deleteUser(TEST_USER_2.getId(), TEST_PASSWORD);
    }

    @BeforeEach
    void createTestShort() {
        Result<Short> shortResult = sqlShorts.createShort(TEST_USER_1.getId(), TEST_PASSWORD);
        assertTrue(shortResult.isOK());
        testShortId = shortResult.value().getId();
    }

    @AfterEach
    void cleanupTestShort() {
        if (testShortId != null) {
            sqlShorts.deleteShort(testShortId, TEST_PASSWORD);
        }
    }

    @Test
    void testCreateShort() {
        Result<Short> result = sqlShorts.createShort(TEST_USER_1.getId(), TEST_PASSWORD);

        assertTrue(result.isOK());
        assertNotNull(result.value());
        assertEquals(TEST_USER_1.userId(), result.value().getOwnerId());
        assertTrue(result.value().getBlobUrl().contains(result.value().getId()));
    }

    @Test
    void testCreateShortWithInvalidCredentials() {
        Result<Short> result = sqlShorts.createShort(TEST_USER_1.getId(), "wrongPassword");

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.UNAUTHORIZED, result.error());
    }

    @Test
    void testGetShort() {
        Result<Short> result = sqlShorts.getShort(testShortId);

        assertTrue(result.isOK());
        assertEquals(testShortId, result.value().getId());
        assertEquals(TEST_USER_1.userId(), result.value().getOwnerId());
    }

    @Test
    void testDeleteShort() {
        Result<Void> deleteResult = sqlShorts.deleteShort(testShortId, TEST_PASSWORD);
        assertTrue(deleteResult.isOK());

        Result<Short> getResult = sqlShorts.getShort(testShortId);
        assertFalse(getResult.isOK());
        assertEquals(Result.ErrorCode.NOT_FOUND, getResult.error());

        testShortId = null; // Prevent cleanup in @AfterEach
    }

    @Test
    void testGetShorts() {
        Result<List<String>> result = sqlShorts.getShorts(TEST_USER_1.getId());

        assertTrue(result.isOK());
        assertFalse(result.value().isEmpty());
        assertTrue(result.value().contains(testShortId));
    }

    @Test
    void testFollowUser() {
        Result<Void> followResult = sqlShorts.follow(TEST_USER_2.getId(), TEST_USER_1.getId(), true, TEST_PASSWORD);
        assertTrue(followResult.isOK());

        Result<List<String>> followersResult = sqlShorts.followers(TEST_USER_1.getId(), TEST_PASSWORD);
        assertTrue(followersResult.isOK());
        assertTrue(followersResult.value().contains(TEST_USER_2.getId()));

        // Cleanup
        sqlShorts.follow(TEST_USER_2.getId(), TEST_USER_1.getId(), false, TEST_PASSWORD);
    }

    @Test
    void testLikeShort() {
        Result<Void> likeResult = sqlShorts.like(testShortId, TEST_USER_2.getId(), true, TEST_PASSWORD);
        assertTrue(likeResult.isOK());

        Result<List<String>> likesResult = sqlShorts.likes(testShortId, TEST_PASSWORD);
        assertTrue(likesResult.isOK());
        assertTrue(likesResult.value().contains(TEST_USER_2.getId()));

        // Cleanup
        sqlShorts.like(testShortId, TEST_USER_2.getId(), false, TEST_PASSWORD);
    }

    @Test
    void testGetFeed() {
        // First follow TEST_USER_1
        sqlShorts.follow(TEST_USER_2.getId(), TEST_USER_1.getId(), true, TEST_PASSWORD);

        Result<List<String>> feedResult = sqlShorts.getFeed(TEST_USER_2.getId(), TEST_PASSWORD);
        assertTrue(feedResult.isOK());
        assertTrue(feedResult.value().contains(testShortId));

        // Cleanup
        sqlShorts.follow(TEST_USER_2.getId(), TEST_USER_1.getId(), false, TEST_PASSWORD);
    }

    @Test
    void testDeleteAllShorts() {
        Result<Void> deleteResult = sqlShorts.deleteAllShorts(TEST_USER_1.getId(), TEST_PASSWORD, createCookie(TEST_USER_1.getId()));
        assertTrue(deleteResult.isOK());

        Result<List<String>> shortsResult = sqlShorts.getShorts(TEST_USER_1.getId());
        assertTrue(shortsResult.isOK());
        assertTrue(shortsResult.value().isEmpty());

        testShortId = null; // Prevent cleanup in @AfterEach
    }

    @Test
    void testConcurrentLikes() throws InterruptedException {
        int numberOfThreads = 10;
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                String userId = "testUser" + threadNum;
                User user = new User(userId, TEST_PASSWORD, userId + "@mail.org", userId);
                sqlUsers.createUser(user);
                sqlShorts.like(testShortId, userId, true, TEST_PASSWORD);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Result<List<String>> likesResult = sqlShorts.likes(testShortId, TEST_PASSWORD);
        assertTrue(likesResult.isOK());
        assertEquals(numberOfThreads, likesResult.value().size());

        // Cleanup
        for (int i = 0; i < numberOfThreads; i++) {
            String userId = "testUser" + i;
            sqlShorts.like(testShortId, userId, false, TEST_PASSWORD);
            sqlUsers.deleteUser(userId, TEST_PASSWORD);
        }
    }
}