package tukano.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tukano.api.Result;
import tukano.api.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLUsersTest {
    private SQLUsers sqlUsers;
    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_PASSWORD = "testpass";
    private static final String TEST_DISPLAY_NAME = "Test User";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeAll
    void setUp() {
        sqlUsers = SQLUsers.getInstance();
        clearTestData();
    }

    @AfterEach
    void tearDown() {
        clearTestData();
    }

    private void clearTestData() {
        // Clean up any test users
        sqlUsers.deleteUser(TEST_USER_ID, TEST_PASSWORD);
        sqlUsers.deleteUser("testuser2", TEST_PASSWORD);
    }

    private User createTestUser() {
        return new User(TEST_USER_ID, TEST_PASSWORD, TEST_DISPLAY_NAME, TEST_EMAIL);
    }

    @Test
    void createUser_Success() {
        User user = createTestUser();
        Result<String> result = sqlUsers.createUser(user);

        assertTrue(result.isOK());
        assertEquals(TEST_USER_ID, result.value());
    }

    @Test
    void createUser_DuplicateUser_ReturnsConflict() {
        User user = createTestUser();
        sqlUsers.createUser(user);
        Result<String> result = sqlUsers.createUser(user);

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.CONFLICT, result.error());
    }

    @Test
    void createUser_InvalidUser_ReturnsBadRequest() {
        User invalidUser = new User(null, TEST_PASSWORD, TEST_DISPLAY_NAME, TEST_EMAIL);
        Result<String> result = sqlUsers.createUser(invalidUser);

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.BAD_REQUEST, result.error());
    }

    @Test
    void getUser_Success() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        Result<User> result = sqlUsers.getUser(TEST_USER_ID, TEST_PASSWORD);

        assertTrue(result.isOK());
        User retrievedUser = result.value();
        assertEquals(TEST_USER_ID, retrievedUser.getId());
        assertEquals(TEST_PASSWORD, retrievedUser.getPwd());
        assertEquals(TEST_DISPLAY_NAME, retrievedUser.getDisplayName());
        assertEquals(TEST_EMAIL, retrievedUser.getEmail());
    }

    @Test
    void getUser_WrongPassword_ReturnsUnauthorized() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        Result<User> result = sqlUsers.getUser(TEST_USER_ID, "wrongpassword");

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.UNAUTHORIZED, result.error());
    }

    @Test
    void getUser_NonexistentUser_ReturnsNotFound() {
        Result<User> result = sqlUsers.getUser("nonexistent", TEST_PASSWORD);

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.NOT_FOUND, result.error());
    }

    @Test
    void getUser_WithCache_ReturnsCachedUser() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        // First call to populate cache
        Result<User> firstResult = sqlUsers.getUser(TEST_USER_ID, TEST_PASSWORD, true);
        assertTrue(firstResult.isOK());

        // Second call should use cache
        Result<User> secondResult = sqlUsers.getUser(TEST_USER_ID, TEST_PASSWORD, true);
        assertTrue(secondResult.isOK());
        assertEquals(firstResult.value().getId(), secondResult.value().getId());
    }

    @Test
    void updateUser_Success() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        User updatedInfo = new User(TEST_USER_ID, TEST_PASSWORD, "Updated Name", "updated@example.com");
        Result<User> result = sqlUsers.updateUser(TEST_USER_ID, TEST_PASSWORD, updatedInfo);

        assertTrue(result.isOK());
        // TODO: fix this - old user value is still in cache
        assertEquals("Updated Name", result.value().getDisplayName());
        assertEquals("updated@example.com", result.value().getEmail());
    }

    @Test
    void updateUser_WrongCredentials_ReturnsNotFound() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        User updatedInfo = new User(TEST_USER_ID, TEST_PASSWORD, "Updated Name", "updated@example.com");
        Result<User> result = sqlUsers.updateUser(TEST_USER_ID, "wrongpass", updatedInfo);

        assertFalse(result.isOK());
        assertEquals(Result.ErrorCode.NOT_FOUND, result.error());
    }

    @Test
    void deleteUser_Success() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        Result<User> deleteResult = sqlUsers.deleteUser(TEST_USER_ID, TEST_PASSWORD);
        assertTrue(deleteResult.isOK());

        // Verify user is deleted
        Result<User> getResult = sqlUsers.getUser(TEST_USER_ID, TEST_PASSWORD);
        // TODO: fix this - the getUser result is okay because the user still exists in the cache
        assertFalse(getResult.isOK());
        assertEquals(Result.ErrorCode.NOT_FOUND, getResult.error());
    }

    @Test
    void searchUsers_MatchingPattern() {
        User user1 = createTestUser();
        User user2 = new User("testuser2", TEST_PASSWORD, "Test User 2", "test2@example.com");

        sqlUsers.createUser(user1);
        sqlUsers.createUser(user2);

        Result<List<User>> result = sqlUsers.searchUsers("test");

        assertTrue(result.isOK());
        List<User> users = result.value();
        assertTrue(users.size() >= 2);
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(TEST_USER_ID)));
        assertTrue(users.stream().anyMatch(u -> u.getId().equals("testuser2")));
        // Verify passwords are not included in search results
        assertTrue(users.stream().allMatch(u -> u.getPwd() == null));
    }

    @Test
    void searchUsers_NoMatches() {
        User user = createTestUser();
        sqlUsers.createUser(user);

        Result<List<User>> result = sqlUsers.searchUsers("nomatch");

        assertTrue(result.isOK());
        assertTrue(result.value().isEmpty());
    }
}