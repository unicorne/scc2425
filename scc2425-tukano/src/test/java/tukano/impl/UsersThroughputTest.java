package tukano.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tukano.api.User;
import tukano.api.Users;
import tukano.impl.users.UsersImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class UsersThroughputTest {

    private final Users users = UsersImpl.getInstance();
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
            users.createUser(user);
        }
    }

    @Test
    public void testThroughputWithCache() throws InterruptedException {
        // Warm-up phase: preload cache
        for (User user : testUsers) {
            users.getUser(user.getId(), user.getPwd(), true);
        }

        int parallelRequests = 50;
        int totalRequests = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            futures.add(executor.submit(() -> {
                long singleStartTime = System.nanoTime();
                users.getUser(randomUser.getId(), randomUser.getPwd(), true);
                return System.nanoTime() - singleStartTime;
            }));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long totalTime = System.nanoTime() - startTime;
        double throughput = (double) totalRequests / (totalTime / 1_000_000_000.0); // Throughput in requests per second

        System.out.println("Throughput with cache: " + throughput + " requests/second");
    }

    @Test
    public void testThroughputWithoutCache() throws InterruptedException {
        int parallelRequests = 50;
        int totalRequests = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            futures.add(executor.submit(() -> {
                long singleStartTime = System.nanoTime();
                users.getUser(randomUser.getId(), randomUser.getPwd(), false);
                return System.nanoTime() - singleStartTime;
            }));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long totalTime = System.nanoTime() - startTime;
        double throughput = (double) totalRequests / (totalTime / 1_000_000_000.0); // Throughput in requests per second

        System.out.println("Throughput without cache: " + throughput + " requests/second");
    }
}

