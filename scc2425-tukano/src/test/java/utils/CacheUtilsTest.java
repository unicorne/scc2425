package utils;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.api.User;
import tukano.api.Short;
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
        assertNotNull(cacheResult.getItem(), "Retrieved user should not be null.");
        assertEquals(testUser.getId(), cacheResult.getItem().getId(), "User ID should match the cached user.");
        assertEquals(testUser.getPwd(), cacheResult.getItem().getPwd(), "User password should match the cached user.");
    }

    @Test
    public void testGetUserFromCache_Miss() {
        // Attempt to retrieve a User that doesn't exist in cache
        CacheUtils.CacheResult<User> cacheResult = cacheUtils.getUserFromCache("nonExistentUser");

        // Assert that the cache hit flag is false and the retrieved user is null
        assertFalse(cacheResult.isCacheHit(), "Cache should miss for a non-existent user.");
        assertNull(cacheResult.getItem(), "Retrieved user should be null for a cache miss.");
    }

    @Test
    public void testStoreShortInCache() {
        // Prepare a test Short object
        Short testShort = new Short("short123", "ownerId", "http://example.com/blob", System.currentTimeMillis(), 10);

        // Store the Short in cache
        cacheUtils.storeShortInCache(testShort);

        // Check that the Short data exists in Redis
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = "short:" + testShort.getId();
            String cachedShortData = jedis.get(cacheKey);

            assertNotNull(cachedShortData, "Short should be stored in Redis cache.");
            // Optionally, additional checks can be added if deserializing and validating the structure is needed
        }
    }

    @Test
    public void testGetShortFromCache_Hit() {
        // Prepare a test Short object
        Short testShort = new Short("short123", "ownerId", "http://example.com/blob", System.currentTimeMillis(), 10);

        // Store the Short in cache to simulate a cache hit
        cacheUtils.storeShortInCache(testShort);

        // Attempt to retrieve the Short from cache
        CacheUtils.CacheResult<Short> cacheResult = cacheUtils.getShortFromCache(testShort.getId());

        // Assert that the cache hit flag is true and the Short is correctly retrieved
        assertTrue(cacheResult.isCacheHit(), "Cache should have a hit for the stored short.");
        assertNotNull(cacheResult.getItem(), "Retrieved short should not be null.");
        assertEquals(testShort.getId(), cacheResult.getItem().getId(), "Short ID should match the cached short.");
        assertEquals(testShort.getOwnerId(), cacheResult.getItem().getOwnerId(), "Owner ID should match the cached short.");
        assertEquals(testShort.getBlobUrl(), cacheResult.getItem().getBlobUrl(), "Blob URL should match the cached short.");
        assertEquals(testShort.getTotalLikes(), cacheResult.getItem().getTotalLikes(), "Total likes should match the cached short.");
    }

    @Test
    public void testGetShortFromCache_Miss() {
        // Attempt to retrieve a Short that doesn't exist in cache
        CacheUtils.CacheResult<Short> cacheResult = cacheUtils.getShortFromCache("nonExistentShort");

        // Assert that the cache hit flag is false and the retrieved short is null
        assertFalse(cacheResult.isCacheHit(), "Cache should miss for a non-existent short.");
        assertNull(cacheResult.getItem(), "Retrieved short should be null for a cache miss.");
    }
}
