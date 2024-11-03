package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.impl.RedisCachePool;

import java.util.logging.Logger;

public class CookieCacheUtils {

    private static final Logger Log = Logger.getLogger(CookieCacheUtils.class.getName());
    private static final String COOKIE_CACHE_PREFIX = "cookie:";

    /**
     * Stores a cookie in the cache with a specified TTL (time to live).
     * @param userId The user ID associated with the cookie.
     * @param cookie The cookie to store.
     * @param ttl The time to live for the cookie in seconds.
     */
    public void storeCookieInCache(String userId, String cookie, int ttl) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = COOKIE_CACHE_PREFIX + userId;
            jedis.setex(cacheKey, ttl, cookie); // Store cookie with TTL
        } catch (Exception e) {
            Log.warning(() -> "Failed to store cookie in cache: " + e.getMessage());
        }
    }

    /**
     * Retrieves a cookie from the cache.
     * @param userId The user ID associated with the cookie.
     * @return The cookie if it exists; otherwise, null.
     */
    public String getCookieFromCache(String userId) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = COOKIE_CACHE_PREFIX + userId;
            return jedis.get(cacheKey);
        } catch (Exception e) {
            Log.warning(() -> "Failed to retrieve cookie from cache: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates if the given cookie matches the one stored in the cache for the user.
     * @param userId The user ID to validate.
     * @param cookie The cookie to validate.
     * @return True if the cookie is valid, false otherwise.
     */
    public boolean isCookieValid(String userId, String cookie) {
        String cachedCookie = getCookieFromCache(userId);
        return cachedCookie != null && cachedCookie.equals(cookie);
    }
}
