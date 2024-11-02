package tukano.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tukano.api.Result;
import tukano.api.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AzureUsersTest {

    private static AzureUsers azureUsers;

    @BeforeAll
    public static void setUp() {
        // Initialize AzureUsers before tests
        azureUsers = new AzureUsers();
    }

    @AfterAll
    public static void tearDown() {
        // Optional: Clean up users created during tests if necessary
    }

    @Test
    public void testCreateUser() {
        User user = new User("1234567", "password123", "test@example.com", "Test User");

        Result<String> result = azureUsers.createUser(user);

        assertTrue(result.isOK(), "User creation should succeed");
        assertEquals(user.getId(), result.value(), "The user ID should match the expected value");
    }

    @Test
    public void testGetUser() {
        User user = new User("getUserTest", "securePass", "Get User Test", "getuser@example.com");
        azureUsers.createUser(user);

        Result<User> result = azureUsers.getUser("getUserTest", "securePass");

        assertTrue(result.isOK(), "User retrieval should succeed");
        assertEquals("getUserTest", result.value().getId(), "Retrieved user ID should match");
        assertEquals("Get User Test", result.value().getDisplayName(), "User display name should match");
    }

    @Test
    public void testUpdateUser() {
        User user = new User("updateUserTest", "password123", "Initial Name", "updateuser@example.com");
        azureUsers.createUser(user);

        User updatedInfo = new User(null, null, "Updated Name", null);

        Result<User> result = azureUsers.updateUser("updateUserTest", "password123", updatedInfo);

        assertTrue(result.isOK(), "User update should succeed");
        assertEquals("Updated Name", result.value().getDisplayName(), "User's display name should be updated");
    }

    @Test
    public void testDeleteUser() {
        User user = new User("deleteUserTest", "deletePass", "Delete User Test", "deleteuser@example.com");
        azureUsers.createUser(user);

        Result<User> result = azureUsers.deleteUser("deleteUserTest", "deletePass");

        assertTrue(result.isOK(), "User deletion should succeed");
        assertEquals("deleteUserTest", result.value().getId(), "Deleted user's ID should match");

        Result<User> getResult = azureUsers.getUser("deleteUserTest", "deletePass");
        assertFalse(getResult.isOK(), "Deleted user should not be retrievable");
        assertEquals(Result.ErrorCode.NOT_FOUND, getResult.error(), "Error code should be NOT_FOUND");
    }

    @Test
    public void testSearchUsers() {
        User user1 = new User("searchTestUser1", "password", "Search User 1", "search1@example.com");
        User user2 = new User("searchTestUser2", "password", "Search User 2", "search2@example.com");
        azureUsers.createUser(user1);
        azureUsers.createUser(user2);

        Result<List<User>> result = azureUsers.searchUsers("searchTestUser");

        assertTrue(result.isOK(), "Search should succeed");
        assertEquals(2, result.value().size(), "Search should return two users");
    }


    @Test
    public void testDatabaseConnection() {
        String query = "SELECT * FROM c"; // Simple query to fetch all items from the Users container

        // Using the searchUsers method with an empty string to simulate a basic read from the database
        Result<List<User>> result = azureUsers.searchUsers("");

        // Check if the result is OK (indicating successful retrieval, even if empty)
        assertTrue(result.isOK(), "Database connection should allow reading data.");
    }

    @Test
    public void testDisplayFirstTwoUsers() {
        // Retrieve all users
        Result<List<User>> result = azureUsers.searchUsers("");

        // Verify that the connection and retrieval were successful
        assertTrue(result.isOK(), "Database connection should allow reading data.");

        List<User> users = result.value();

        // Display the first two users if they exist
        if (users.size() >= 2) {
            System.out.println("First user: " + users.get(0));
            System.out.println("Second user: " + users.get(1));
        } else if (users.size() == 1) {
            System.out.println("Only one user available: " + users.get(0));
        } else {
            System.out.println("No users available in the database.");
        }
    }
}

