package utils;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.api.User;
import tukano.impl.RedisCachePool;
import utils.CacheUtils.CacheResult;

import static org.junit.jupiter.api.Assertions.*;

public class CacheUtilsTest {

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
        CacheUtils.storeUserInCache(testUser);

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
        CacheUtils.storeUserInCache(testUser);

        // Attempt to retrieve the User from cache
        CacheResult<User> cacheResult = CacheUtils.getUserFromCache(testUser.getId());

        // Assert that the cache hit flag is true and the user is correctly retrieved
        assertTrue(cacheResult.isCacheHit(), "Cache should have a hit for the stored user.");
        assertNotNull(cacheResult.getObject(), "Retrieved user should not be null.");
        assertEquals(testUser.getId(), cacheResult.getObject().getId(), "User ID should match the cached user.");
        assertEquals(testUser.getPwd(), cacheResult.getObject().getPwd(), "User password should match the cached user.");
    }

    @Test
    public void testGetUserFromCache_Miss() {
        // Attempt to retrieve a User that doesn't exist in cache
        CacheResult<User> cacheResult = CacheUtils.getUserFromCache("nonExistentUser");

        // Assert that the cache hit flag is false and the retrieved user is null
        assertFalse(cacheResult.isCacheHit(), "Cache should miss for a non-existent user.");
        assertNull(cacheResult.getObject(), "Retrieved user should be null for a cache miss.");
    }

    @Test
    public void testRemoveUserFromCache() {
        // Prepare a test User object
        User testUser = new User();
        testUser.setId("testUserToRemove");
        testUser.setPwd("testPassword");

        // Store the User in cache
        CacheUtils.storeUserInCache(testUser);

        // Ensure the User is in cache
        CacheResult<User> cacheResultBeforeRemove = CacheUtils.getUserFromCache(testUser.getId());
        assertTrue(cacheResultBeforeRemove.isCacheHit(), "Cache should have a hit for the stored user before removal.");

        // Remove the User from cache
        CacheUtils.removeUserFromCache(testUser.getId());

        // Attempt to retrieve the User from cache after removal
        CacheResult<User> cacheResultAfterRemove = CacheUtils.getUserFromCache(testUser.getId());

        // Assert that the cache miss flag is true and no user is retrieved
        assertFalse(cacheResultAfterRemove.isCacheHit(), "Cache should miss for the removed user.");
        assertNull(cacheResultAfterRemove.getObject(), "Retrieved user should be null after cache removal.");
    }

    @Test
    public void testStoreAndRetrieveUser() {
        // Create test user
        User testUser = new User("testUserId", "username", "password", "email");

        // Store user in cache
        CacheUtils.storeUserInCache(testUser);

        // Retrieve user from cache
        CacheUtils.CacheResult<User> result = CacheUtils.getUserFromCache(testUser.getId());

        // Verify cache retrieval
        assertTrue(result.isCacheHit());
        assertNotNull(result.getObject());
        assertEquals(testUser.getId(), result.getObject().getId());
        assertEquals(testUser.getDisplayName(), result.getObject().getDisplayName());
    }

    @Test
    public void testStoreAndRetrieveSession() {
        // Create test session
        Session testSession = new Session("sessionId", "userId");

        // Store session in cache
        CacheUtils.storeSessionInCache(testSession);

        // Retrieve session from cache
        CacheUtils.CacheResult<Session> result = CacheUtils.getSessionFromCache(testSession.uuid());

        // Verify cache retrieval
        assertTrue(result.isCacheHit());
        assertNotNull(result.getObject());
        assertEquals(testSession.uuid(), result.getObject().uuid());
    }

    @Test
    public void testStoreAndRetrieveToken() {
        // Prepare test token data
        String userId = "testUserId";
        String token = "testToken123";

        // Store token in cache
        CacheUtils.storeTokenInCache(userId, token);

        // Retrieve token from cache
        CacheUtils.CacheResult<String> result = CacheUtils.getTokenFromCache(userId);

        // Verify cache retrieval
        assertTrue(result.isCacheHit());
        assertNotNull(result.getObject());
        assertEquals(token, result.getObject());
    }

    @Test
    public void testCacheMiss() {
        // Attempt to retrieve non-existent user
        CacheUtils.CacheResult<User> result = CacheUtils.getUserFromCache("nonExistentUserId");

        // Verify cache miss
        assertFalse(result.isCacheHit());
        assertNull(result.getObject());
    }
}
