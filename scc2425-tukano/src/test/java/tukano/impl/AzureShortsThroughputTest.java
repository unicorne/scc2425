package tukano.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tukano.api.Short;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class AzureShortsThroughputTest {

    private final AzureShorts shorts = AzureShorts.getInstance();
    private final List<Short> testShorts = new ArrayList<>();
    private final Random random = new Random();

    @BeforeEach
    public void setUp() {
        // Create 20 random short videos
        for (int i = 1; i <= 20; i++) {
            String shortId = "short" + i;
            String ownerId = "owner" + i;
            String blobUrl = "http://example.com/blob" + i;
            long timestamp = System.currentTimeMillis();
            int totalLikes = random.nextInt(100); // Random likes count

            Short shrt = new Short(shortId, ownerId, blobUrl, timestamp, totalLikes);
            testShorts.add(shrt);

            // Assuming AzureShorts#createShort takes userId and pwd parameters instead of a Short object
            shorts.createShort(ownerId, "password" + i);
        }
    }

    @Test
    public void testThroughputWithCache() throws InterruptedException {
        // Warm-up phase: preload cache
        for (Short shrt : testShorts) {
            shorts.getShort(shrt.getId(), true);
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
                shorts.getShort(randomShort.getId(), true);
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
                shorts.getShort(randomShort.getId(), false);
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