package tukano.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tukano.api.Result;
import tukano.api.Short;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class AzureShortsThroughputTest {

    private final AzureShorts azureShorts = AzureShorts.getInstance();
    private final List<Short> testShorts = new ArrayList<>();
    private final Random random = new Random();

    @BeforeEach
    public void setUp() {
        // Create 20 random shorts by calling createShort with userId and password
        for (int i = 1; i <= 20; i++) {
            String userId = "user" + i;
            String password = "password" + i;

            // Use createShort to add the short and store its ID for later tests
            Result<Short> result = azureShorts.createShort(userId, password);

            // Check if the result is successful before adding to testShorts
            if (result.isOK()) {
                testShorts.add(result.value());
            } else {
                System.err.println("Failed to create short for user: " + userId + ". Error: " + result.error());
            }
        }
    }

    @Test
    public void testThroughputWithCache() throws InterruptedException {
        // Warm-up phase: preload cache
        for (Short shrt : testShorts) {
            azureShorts.getShort(shrt.getId());
        }

        int parallelRequests = 50;
        int totalRequests = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            Short randomShort = testShorts.get(random.nextInt(testShorts.size()));
            futures.add(executor.submit(() -> {
                long singleStartTime = System.nanoTime();
                azureShorts.getShort(randomShort.getId());
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
            Short randomShort = testShorts.get(random.nextInt(testShorts.size()));
            futures.add(executor.submit(() -> {
                long singleStartTime = System.nanoTime();
                azureShorts.getShort(randomShort.getId());
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
