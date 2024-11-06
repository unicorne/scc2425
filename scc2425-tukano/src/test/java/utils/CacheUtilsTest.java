package utils;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.api.User;
import tukano.impl.RedisCachePool;
import utils.CacheUtils;
import static org.junit.jupiter.api.Assertions.*;

public class CacheUtilsTest {

    private final CacheUtils cacheUtils = new CacheUtils();

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
    public void testStoreUserInCache() {
        // Prepare a test User object
        User testUser = new User();
        testUser.setId("testUser123");
        testUser.setPwd("testPassword");

        // Store the User in cache
        cacheUtils.storeUserInCache(testUser);

        // Check that the User data exists in Redis
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = "user:" + testUser.getId();
            String cachedUserData = jedis.get(cacheKey);

            assertNotNull(cachedUserData, "User should be stored in Redis cache.");
            // Optionally, further checks can be added if you serialize the user to JSON and check the structure
        }
    }

    @Test
    public void testGetUserFromCache_Hit() {
        // Prepare a test User object
        User testUser = new User();
        testUser.setId("testUser123");
        testUser.setPwd("testPassword");

        // Store the User in cache to simulate a cache hit
        cacheUtils.storeUserInCache(testUser);

        // Attempt to retrieve the User from cache
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache(testUser.getId());

        // Assert that the cache hit flag is true and the user is correctly retrieved
        assertTrue(cacheResult.isCacheHit(), "Cache should have a hit for the stored user.");
        assertNotNull(cacheResult.getUser(), "Retrieved user should not be null.");
        assertEquals(testUser.getId(), cacheResult.getUser().getId(), "User ID should match the cached user.");
        assertEquals(testUser.getPwd(), cacheResult.getUser().getPwd(), "User password should match the cached user.");
    }

    @Test
    public void testGetUserFromCache_Miss() {
        // Attempt to retrieve a User that doesn't exist in cache
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache("nonExistentUser");

        // Assert that the cache hit flag is false and the retrieved user is null
        assertFalse(cacheResult.isCacheHit(), "Cache should miss for a non-existent user.");
        assertNull(cacheResult.getUser(), "Retrieved user should be null for a cache miss.");
    }

    @Test
    public void testRemoveUserFromCache() {
        // Prepare a test User object
        User testUser = new User();
        testUser.setId("testUserToRemove");
        testUser.setPwd("testPassword");

        // Store the User in cache
        cacheUtils.storeUserInCache(testUser);

        // Ensure the User is in cache
        CacheUtils.CacheResult<User> cacheResultBeforeRemove = cacheUtils.getUserFromCache(testUser.getId());
        assertTrue(cacheResultBeforeRemove.isCacheHit(), "Cache should have a hit for the stored user before removal.");

        // Remove the User from cache
        cacheUtils.removeUserFromCache(testUser.getId());

        // Attempt to retrieve the User from cache after removal
        CacheUtils.CacheResult<User> cacheResultAfterRemove = cacheUtils.getUserFromCache(testUser.getId());

        // Assert that the cache miss flag is true and no user is retrieved
        assertFalse(cacheResultAfterRemove.isCacheHit(), "Cache should miss for the removed user.");
        assertNull(cacheResultAfterRemove.getUser(), "Retrieved user should be null after cache removal.");
    }
}
