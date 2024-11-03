package tukano.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tukano.api.Short;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AzureShortsLatencyTest {

    private final AzureShorts azureShorts = AzureShorts.getInstance();
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

            // Insert each short into the database (simulated by createShort with ownerId and password)
            azureShorts.createShort(ownerId, "password" + i);
        }
    }

    @AfterEach
    public void tearDown() {
        // Remove each test short from the database or cache
        for (int i = 0; i < testShorts.size(); i++) {
            Short shrt = testShorts.get(i);
            azureShorts.deleteShort(shrt.getId(), "password" + (i + 1)); // Providing both id and password
        }
        testShorts.clear(); // Clear the list for the next test
    }

    @Test
    public void testRandomShortRetrievalWithCachePerformance() {
        // Warm-up phase (optional)
        for (Short shrt : testShorts) {
            azureShorts.getShort(shrt.getId(), true);
        }

        long totalDurationWithCache = 0;
        int retrievalCount = 100;

        // Randomly retrieve shorts with caching enabled
        for (int i = 0; i < retrievalCount; i++) {
            Short randomShort = testShorts.get(random.nextInt(testShorts.size()));
            long startTime = System.nanoTime();
            azureShorts.getShort(randomShort.getId(), true);
            totalDurationWithCache += System.nanoTime() - startTime;
        }

        long averageDurationWithCache = totalDurationWithCache / retrievalCount;
        System.out.println("Average execution time with cache: " + averageDurationWithCache + " ns");
    }

    @Test
    public void testRandomShortRetrievalWithoutCachePerformance() {
        long totalDurationWithoutCache = 0;
        int retrievalCount = 100;

        // Randomly retrieve shorts with caching disabled
        for (int i = 0; i < retrievalCount; i++) {
            Short randomShort = testShorts.get(random.nextInt(testShorts.size()));
            long startTime = System.nanoTime();
            azureShorts.getShort(randomShort.getId(), false);
            totalDurationWithoutCache += System.nanoTime() - startTime;
        }

        long averageDurationWithoutCache = totalDurationWithoutCache / retrievalCount;
        System.out.println("Average execution time without cache: " + averageDurationWithoutCache + " ns");
    }
}
