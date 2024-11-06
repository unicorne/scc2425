package tukano.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tukano.api.Result;
import tukano.api.User;
import utils.CacheUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AzureUsersTest {

    private static AzureUsers azureUsers;
    private final User testUser1 = new User("testUser1", "password123", "testuser1@mail.org", "Test User 1");
    private final User testUser2 = new User("testUser2", "password123", "testuser2@mail.org", "Test User 2");
    private final CacheUtils cacheUtils = new CacheUtils();
    private final User testUser = new User("testUserCache", "password123", "cacheuser@mail.org", "Cache User");

    @BeforeAll
    public static void setUp() {
        azureUsers = AzureUsers.getInstance();
    }

    @AfterEach
    void deleteUsers() {
        azureUsers.deleteUser(testUser1.getId(), testUser1.getPwd());
        azureUsers.deleteUser(testUser2.getId(), testUser2.getPwd());
    }

    @Test
    public void testCreateUser() {
        Result<String> result = azureUsers.createUser(testUser1);
        assertTrue(result.isOK(), "User creation should succeed");
        assertEquals(testUser1.getId(), result.value(), "The user ID should match the expected value");
    }

    @Test
    public void testGetUser() {
        azureUsers.createUser(testUser1);

        Result<User> result = azureUsers.getUser(testUser1.userId(), testUser1.getPwd());

        assertTrue(result.isOK(), "User retrieval should succeed");
        assertEquals(testUser1.getId(), result.value().getId(), "Retrieved user ID should match");
        assertEquals(testUser1.getPwd(), result.value().getPwd(), "User pwd should match");
        assertEquals(testUser1.getEmail(), result.value().getEmail(), "User email should match");
        assertEquals(testUser1.getDisplayName(), result.value().getDisplayName(), "User display name should match");
    }

    @Test
    public void testUpdateUser() {
        String updatedName = "Updated Name";
        String updatedEmail = "ab@c.de";
        azureUsers.createUser(testUser1);

        User updatedInfo = new User(null, null, updatedEmail, updatedName);

        Result<User> result = azureUsers.updateUser(testUser1.userId(), testUser1.getPwd(), updatedInfo);

        assertTrue(result.isOK(), "User update should succeed");
        assertEquals(updatedEmail, result.value().getEmail(), "User's email should be updated");
        assertEquals(updatedName, result.value().getDisplayName(), "User's display name should be updated");
    }

    @Test
    public void testDeleteUser() {
        azureUsers.createUser(testUser1);

        Result<User> result = azureUsers.deleteUser(testUser1.getId(), testUser1.getPwd());

        assertTrue(result.isOK(), "User deletion should succeed");
        assertEquals(testUser1.userId(), result.value().getId(), "Deleted user's ID should match");

        Result<User> getResult = azureUsers.getUser(testUser1.getId(), testUser1.getPwd());
        assertFalse(getResult.isOK(), "Deleted user should not be retrievable");
        assertEquals(Result.ErrorCode.NOT_FOUND, getResult.error(), "Error code should be NOT_FOUND");
    }

    @Test
    public void testSearchUsers() {
        azureUsers.createUser(testUser1);
        azureUsers.createUser(testUser2);

        Result<List<User>> result = azureUsers.searchUsers("testuser");

        assertTrue(result.isOK(), "Search should succeed");
        assertEquals(2, result.value().size(), "Search should return two users");
        List<String> resultIds = result.value().stream().map(User::getId).toList();
        assertTrue(resultIds.contains(testUser1.getId()), "First user should be in the search results");
        assertTrue(resultIds.contains(testUser2.getId()), "Second user should be in the search results");
    }

    @Test
    public void testUserInCacheAfterGetUser() {
        // Create the user in the database
        azureUsers.createUser(testUser);

        // Retrieve the user to trigger caching
        Result<User> result = azureUsers.getUser(testUser.getId(), testUser.getPwd());

        assertTrue(result.isOK(), "User retrieval should succeed");

        // Check if the user is in cache
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache(testUser.getId());
        assertTrue(cacheResult.isCacheHit(), "User should be in cache after retrieval");
        assertEquals(testUser.getId(), cacheResult.getUser().getId(), "Cached user ID should match");
    }

    @Test
    public void testUserNotInCacheAfterDeleteUser() {
        // Create and retrieve the user to populate the cache
        azureUsers.createUser(testUser);
        azureUsers.getUser(testUser.getId(), testUser.getPwd());

        // Delete the user
        azureUsers.deleteUser(testUser.getId(), testUser.getPwd());

        // Check if the user is removed from cache
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache(testUser.getId());
        assertFalse(cacheResult.isCacheHit(), "User should not be in cache after deletion");
    }

    @Test
    public void testCacheUpdatedAfterUpdateUser() {
        // Create and retrieve the user to populate the cache
        azureUsers.createUser(testUser);
        azureUsers.getUser(testUser.getId(), testUser.getPwd());

        // Prepare updated user information
        String updatedEmail = "updated@mail.org";
        String updatedDisplayName = "Updated Cache User";
        User updatedInfo = new User(null, null, updatedEmail, updatedDisplayName);

        // Update the user
        azureUsers.updateUser(testUser.getId(), testUser.getPwd(), updatedInfo);

        // Check if the cache contains the updated user information
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache(testUser.getId());
        assertTrue(cacheResult.isCacheHit(), "Cache should be updated with new user information");
        assertEquals(updatedEmail, cacheResult.getUser().getEmail(), "Cached user email should be updated");
        assertEquals(updatedDisplayName, cacheResult.getUser().getDisplayName(), "Cached user display name should be updated");
    }
}

