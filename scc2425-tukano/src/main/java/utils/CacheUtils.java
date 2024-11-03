package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.impl.RedisCachePool;
import tukano.api.User;
import tukano.api.Short;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class CacheUtils {

    private static final Logger Log = Logger.getLogger(CacheUtils.class.getName());
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String SHORT_CACHE_PREFIX = "short:";
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON

    public CacheResult<User> getUserFromCache(String userId) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = "user:" + userId;
            String cachedUserData = jedis.get(cacheKey);

            if (cachedUserData != null) {
                // Deserialize only if cache data is found
                User cachedUser = objectMapper.readValue(cachedUserData, User.class);
                return new CacheResult<>(cachedUser, true);
            } else {
                return new CacheResult<>(null, false);
            }
        } catch (Exception e) {
            // Handle exceptions appropriately (e.g., log and return a cache miss)
            return new CacheResult<>(null, false);
        }
    }

    public void storeUserInCache(User user) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = "user:" + user.getId();
            String serializedUserData = objectMapper.writeValueAsString(user);
            jedis.setex(cacheKey, 3600, serializedUserData); // Set 1-hour TTL
        } catch (Exception e) {
            // Handle exceptions appropriately
        }
    }

    // Method to retrieve Short from cache
    public CacheResult<Short> getShortFromCache(String shortId) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = SHORT_CACHE_PREFIX + shortId;
            String cachedShortData = jedis.get(cacheKey);

            if (cachedShortData != null) {
                // Deserialize only if cache data is found
                Short cachedShort = objectMapper.readValue(cachedShortData, Short.class);
                return new CacheResult<>(cachedShort, true);
            } else {
                return new CacheResult<>(null, false);
            }
        } catch (Exception e) {
            Log.warning("Failed to retrieve Short from cache: " + e.getMessage());
            return new CacheResult<>(null, false);
        }
    }

    // Method to store Short in cache
    public void storeShortInCache(Short shrt) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = SHORT_CACHE_PREFIX + shrt.getId();
            String serializedShortData = objectMapper.writeValueAsString(shrt);
            jedis.setex(cacheKey, 3600, serializedShortData); // Set 1-hour TTL
        } catch (Exception e) {
            Log.warning("Failed to store Short in cache: " + e.getMessage());
        }
    }


    /**
     * Serialize a User object to JSON String.
     */
    public String serializeUser(User user) {
        try {
            return objectMapper.writeValueAsString(user); // Convert User to JSON String
        } catch (JsonProcessingException e) {
            Log.warning(() -> "Serialization error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize a JSON String to a User object.
     */
    public User deserializeUser(String data) {
        try {
            return objectMapper.readValue(data, User.class); // Convert JSON String back to User
        } catch (JsonProcessingException e) {
            Log.warning(() -> "Deserialization error: " + e.getMessage());
            return null;
        }
    }


    /**
     * A helper class to return the cache result along with a hit/miss indicator.
     */
    public static class CacheResult<T> {
        private final T user;
        private final boolean cacheHit;

        public CacheResult(T user, boolean cacheHit) {
            this.user = user;
            this.cacheHit = cacheHit;
        }

        public T getUser() {
            return user;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }
    }
}
