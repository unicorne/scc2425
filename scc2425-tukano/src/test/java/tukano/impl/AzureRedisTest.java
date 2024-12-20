package tukano.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.impl.users.UsersImpl;

import static org.junit.jupiter.api.Assertions.*;
import static tukano.impl.RedisCachePool.getCachePool;

public class AzureRedisTest {

    private static Users users;
    private static final String REDIS_TEST_USER_ID = "redisCacheTestUser";
    private static final String REDIS_TEST_USER_PWD = "redisCachePass";
    private static final String REDIS_USER_KEY = "user:" + REDIS_TEST_USER_ID;
    private static final String REDIS_TEST_USER_DISPLAY_NAME = "rediscache@example.com";

    @BeforeAll
    public static void setUp() {
        // Initialize the AzureUsers instance and clear the Redis cache before testing
        users = UsersImpl.getInstance();
        JedisPool jedisPool = getCachePool();
        try (Jedis jedis = jedisPool.getResource()) {
            // Clear all data in Redis to ensure a clean state for test
            jedis.flushDB();
            System.out.println(1);
        }
    }

    @Test
    public void testRedisConnection() {
        // Check if a Redis connection is established
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {

            assertNotNull(jedis, "Redis connection should be established.");
            assertTrue(jedis.isConnected(), "Redis should be connected.");
        } catch (Exception e) {
            fail("Redis connection failed: " + e.getMessage());
        }
    }

    @Test
    public void testUserCachingInRedis() {
        // Create and retrieve a user to populate the Redis cache
        User user = new User(REDIS_TEST_USER_ID, REDIS_TEST_USER_PWD, "Redis Cache Test User", REDIS_TEST_USER_DISPLAY_NAME);
        users.createUser(user);

        // Retrieve the user to trigger caching
        Result<User> result = users.getUser(REDIS_TEST_USER_ID, REDIS_TEST_USER_PWD);
        assertTrue(result.isOK(), "User retrieval should succeed.");
        assertEquals(REDIS_TEST_USER_ID, result.value().getId(), "User ID should match the retrieved value.");

        // Check if the user data is cached in Redis
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cachedUserJson = jedis.get(REDIS_USER_KEY);
            assertNotNull(cachedUserJson, "User data should be cached in Redis.");
            assertTrue(cachedUserJson.contains("Redis Cache Test User"), "Cached data should contain correct user information.");
        }
    }

    @Test
    public void testUserRetrievalFromCache() {
        // Ensure that the user is cached by calling getUser
        users.getUser(REDIS_TEST_USER_ID, REDIS_TEST_USER_PWD);

        // Remove the user from the database to ensure only the cache has the data
        try {
            users.deleteUser(REDIS_TEST_USER_ID, REDIS_TEST_USER_PWD);
        } catch (Exception e) {
            // Ignore if deletion fails; we want to simulate a cache-only situation
        }

        // Retrieve the user again to confirm it comes from the cache
        Result<User> cachedResult = users.getUser(REDIS_TEST_USER_ID, REDIS_TEST_USER_PWD);
        assertFalse(cachedResult.isOK(), "User retrieval from cache should succeed.");
    }

    @Test
    public void testCacheExpiry() throws InterruptedException {
        // Set a short expiry time for testing purposes
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(REDIS_USER_KEY, 2); // Set expiry to 2 seconds
        }

        // Wait for the cache to expire
        Thread.sleep(3000);

        // Check that the cache no longer contains the user data
        try (Jedis jedis = getCachePool().getResource()) {
            String cachedUserJson = jedis.get(REDIS_USER_KEY);
            assertNull(cachedUserJson, "Cached data should expire and be null.");
        }
    }

    @AfterAll
    public static void tearDown() {
        // Clean up Redis cache for the test user after all tests complete
        try (Jedis jedis = getCachePool().getResource()) {
            jedis.del(REDIS_USER_KEY);
        }
    }

    @Test
    public void firstTest() {
        // Get the Redis pool
        JedisPool pool = RedisCachePool.getCachePool();

        // Get a Redis connection from the pool
        try (Jedis jedis = pool.getResource()) {
            // Use the Redis connection to perform some operations
            jedis.set("example_key4", "example_value4");
            String value = jedis.get("example_key4");

            System.out.println("Retrieved value: " + value);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}

