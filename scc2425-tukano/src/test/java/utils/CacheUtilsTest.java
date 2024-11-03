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
    public void testStoreShortInCache() {
        // Prepare a test Short object
        Short testShort = new Short();
        testShort.setId("testShort123");
        testShort.setOwnerId("owner456");
        testShort.setBlobUrl("http://example.com/blob.mp4");
        testShort.setTimestamp(System.currentTimeMillis());
        testShort.setTotalLikes(100);

        // Store the Short in cache
        cacheUtils.storeShortInCache(testShort);

        // Check that the Short data exists in Redis
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = "short:" + testShort.getId();
            String cachedShortData = jedis.get(cacheKey);

            assertNotNull(cachedShortData, "Short should be stored in Redis cache.");
            // Optionally, further checks can be added if you serialize the short to JSON and check the structure
        }
    }

    @Test
    public void testGetShortFromCache_Hit() {
        // Prepare a test Short object
        Short testShort = new Short();
        testShort.setId("testShort123");
        testShort.setOwnerId("owner456");
        testShort.setBlobUrl("http://example.com/blob.mp4");
        testShort.setTimestamp(System.currentTimeMillis());
        testShort.setTotalLikes(100);

        // Store the Short in cache to simulate a cache hit
        cacheUtils.storeShortInCache(testShort);

        // Attempt to retrieve the Short from cache
        CacheUtils.CacheResult<Short> cacheResult = cacheUtils.getShortFromCache(testShort.getId());

        // Assert that the cache hit flag is true and the Short is correctly retrieved
        assertTrue(cacheResult.isCacheHit(), "Cache should have a hit for the stored Short.");
        assertNotNull(cacheResult.getUser(), "Retrieved Short should not be null.");
        assertEquals(testShort.getId(), cacheResult.getUser().getId(), "Short ID should match the cached Short.");
        assertEquals(testShort.getOwnerId(), cacheResult.getUser().getOwnerId(), "Short owner ID should match the cached Short.");
        assertEquals(testShort.getBlobUrl(), cacheResult.getUser().getBlobUrl(), "Short blob URL should match the cached Short.");
        assertEquals(testShort.getTotalLikes(), cacheResult.getUser().getTotalLikes(), "Short total likes should match the cached Short.");
    }

    @Test
    public void testGetShortFromCache_Miss() {
        // Attempt to retrieve a Short that doesn't exist in cache
        CacheUtils.CacheResult<Short> cacheResult = cacheUtils.getShortFromCache("nonExistentShort");

        // Assert that the cache hit flag is false and the retrieved Short is null
        assertFalse(cacheResult.isCacheHit(), "Cache should miss for a non-existent Short.");
        assertNull(cacheResult.getUser(), "Retrieved Short should be null for a cache miss.");
    }
}
