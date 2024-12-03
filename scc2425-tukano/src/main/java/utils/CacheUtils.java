package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.impl.RedisCachePool;
import tukano.api.User;

import java.util.logging.Logger;

public class CacheUtils {

    private static final Logger Log = Logger.getLogger(CacheUtils.class.getName());
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON

    public static CacheResult<User> getUserFromCache(String userId) {
        return getFromCache(USER_CACHE_PREFIX + userId, User.class);
    }

    public static CacheResult<String> getTokenFromCache(String userId) {
        return getFromCache(TOKEN_CACHE_PREFIX + userId, String.class);
    }

    public static CacheResult<Session> getSessionFromCache(String sessionId) {
        return getFromCache(SESSION_CACHE_PREFIX + sessionId, Session.class);
    }

    private static <T> CacheResult<T> getFromCache(String cacheKey, Class<T> clazz) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String cachedData = jedis.get(cacheKey);

            if (cachedData != null) {
                // Deserialize only if cache data is found
                T cachedObject = deserializeObject(cachedData, clazz);
                return new CacheResult<>(cachedObject, true);
            } else {
                return new CacheResult<>(null, false);
            }
        } catch (Exception e) {
            Log.warning(() -> "Cache read error: " + e.getMessage());
            return new CacheResult<>(null, false);
        }
    }

    public static void storeUserInCache(User user) {
        storeinCache(USER_CACHE_PREFIX + user.getId(), user);
    }

    public static void storeSessionInCache(Session session){
        storeinCache(SESSION_CACHE_PREFIX + session.uuid(), session);
    }

    public static void storeTokenInCache(String userId, String token) {
        storeinCache(TOKEN_CACHE_PREFIX + userId, token);
    }

    private static void storeinCache(String cacheKey, Object object){
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            String serializedSessionData = serializeObject(object);
            jedis.setex(cacheKey, 3600, serializedSessionData); // Set 1-hour TTL
        } catch (Exception e) {
            Log.severe(() -> "Cache write error: " + e.getMessage());
        }
    }

    public static void removeUserFromCache(String userId) {
        removeFromCache(USER_CACHE_PREFIX + userId);
    }

    private static void removeFromCache(String cacheKey) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            jedis.del(cacheKey);
            Log.info(() -> String.format("Cache entry removed for key %s", cacheKey));
        } catch (Exception e) {
            Log.warning(() -> String.format("Error removing key from cache: %s", e.getMessage()));
        }
    }

    /**
     * Serialize a User object to JSON String.
     */
    public static <T> String serializeObject(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Log.warning(() -> "Serialization error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize a JSON String to a User object.
     */
    public static <T> T deserializeObject(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            Log.warning(() -> "Deserialization error: " + e.getMessage());
            return null;
        }
    }


    /**
     * A helper class to return the cache result along with a hit/miss indicator.
     */
    public static class CacheResult<T> {
        private final T obj;
        private final boolean cacheHit;

        public CacheResult(T obj, boolean cacheHit) {
            this.obj = obj;
            this.cacheHit = cacheHit;
        }

        public T getObject() {
            return obj;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }
    }
}
