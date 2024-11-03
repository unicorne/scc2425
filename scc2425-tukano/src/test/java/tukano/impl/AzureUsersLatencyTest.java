package tukano.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tukano.api.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AzureUsersLatencyTest {

    private final AzureUsers azureUsers = AzureUsers.getInstance();
    private final List<User> testUsers = new ArrayList<>();
    private final Random random = new Random();

    @BeforeEach
    public void setUp() {
        // Create 20 random users
        for (int i = 1; i <= 20; i++) {
            String userId = "user" + i;
            String password = "password" + i;
            String email = "user" + i + "@example.com";
            String displayName = "User " + i;
            User user = new User(userId, password, email, displayName);
            testUsers.add(user);

            // Insert each user into the database (simulated by createUser)
            azureUsers.createUser(user);
        }
    }

    @AfterEach
    public void tearDown() {
        // Remove each test user from the database or cache
        for (User user : testUsers) {
            azureUsers.deleteUser(user.getId(), user.getPwd()); // Ensure deleteUser is implemented
        }
        testUsers.clear(); // Clear the list for the next test
    }

    @Test
    public void testRandomUserRetrievalWithCachePerformance() {
        // Warm-up phase (optional)
        for (User user : testUsers) {
            azureUsers.getUser(user.getId(), user.getPwd(), true);
        }

        long totalDurationWithCache = 0;
        int retrievalCount = 100;

        // Randomly retrieve users with caching enabled
        for (int i = 0; i < retrievalCount; i++) {
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            long startTime = System.nanoTime();
            azureUsers.getUser(randomUser.getId(), randomUser.getPwd(), true);
            totalDurationWithCache += System.nanoTime() - startTime;
        }

        long averageDurationWithCache = totalDurationWithCache / retrievalCount;
        System.out.println("Average execution time with cache: " + averageDurationWithCache + " ns");
    }

    @Test
    public void testRandomUserRetrievalWithoutCachePerformance() {
        long totalDurationWithoutCache = 0;
        int retrievalCount = 100;

        // Randomly retrieve users with caching disabled
        for (int i = 0; i < retrievalCount; i++) {
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            long startTime = System.nanoTime();
            azureUsers.getUser(randomUser.getId(), randomUser.getPwd(), false);
            totalDurationWithoutCache += System.nanoTime() - startTime;
        }

        long averageDurationWithoutCache = totalDurationWithoutCache / retrievalCount;
        System.out.println("Average execution time without cache: " + averageDurationWithoutCache + " ns");
    }
}
