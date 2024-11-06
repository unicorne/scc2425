package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.impl.RedisCachePool;
import tukano.api.User;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class CacheUtils {

    private static final Logger Log = Logger.getLogger(CacheUtils.class.getName());
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON

    public static CacheResult<User> getUserFromCache(String userId) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = USER_CACHE_PREFIX + userId;
            String cachedUserData = jedis.get(cacheKey);

            if (cachedUserData != null) {
                // Deserialize only if cache data is found
                User cachedUser = deserializeUser(cachedUserData);
                return new CacheResult<>(cachedUser, true);
            } else {
                return new CacheResult<>(null, false);
            }
        } catch (Exception e) {
            Log.warning(() -> "Cache read error: " + e.getMessage());
            return new CacheResult<>(null, false);
        }
    }

    public static void storeUserInCache(User user) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = USER_CACHE_PREFIX + user.getId();
            String serializedUserData = serializeUser(user);
            jedis.setex(cacheKey, 3600, serializedUserData); // Set 1-hour TTL
        } catch (Exception e) {
            Log.severe(() -> "Cache write error: " + e.getMessage());
        }
    }

    public static void storeTokenInCache(String userId, String token) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = TOKEN_CACHE_PREFIX + userId;
            jedis.setex(cacheKey, 3600, token); // Set 1-hour TTL
        } catch (Exception e) {
            Log.severe(() -> "Cache write error: " + e.getMessage());
        }
    }

    public static CacheResult<String> getTokenFromCache(String userId) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = TOKEN_CACHE_PREFIX + userId;
            String token = jedis.get(cacheKey);

            if (token != null) {
                return new CacheResult<>(token, true);
            } else {
                return new CacheResult<>(null, false);
            }
        } catch (Exception e) {
            Log.warning(() -> "Cache read error: " + e.getMessage());
            return new CacheResult<>(null, false);
        }
    }

    /**
     * Serialize a User object to JSON String.
     */
    public static String serializeUser(User user) {
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
    public static User deserializeUser(String data) {
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
