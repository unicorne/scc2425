package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.impl.RedisCachePool;

import java.util.logging.Logger;

public class BinaryCacheUtils {

    private static final Logger Log = Logger.getLogger(BinaryCacheUtils.class.getName());

    public CacheResult<byte[]> getFromCache(String cacheKey) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            byte[] cachedData = jedis.get(cacheKey.getBytes());
            return cachedData != null ? new CacheResult<>(cachedData, true) : new CacheResult<>(null, false);
        } catch (Exception e) {
            Log.warning(() -> "Cache retrieval error: " + e.getMessage());
            return new CacheResult<>(null, false);
        }
    }

    public void storeInCache(String cacheKey, byte[] data, int ttl) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(cacheKey.getBytes(), ttl, data);
        } catch (Exception e) {
            Log.warning(() -> "Cache storage error: " + e.getMessage());
        }
    }

    public void deleteFromCache(String cacheKey) {
        JedisPool pool = RedisCachePool.getCachePool();
        try (Jedis jedis = pool.getResource()) {
            jedis.del(cacheKey.getBytes());
        } catch (Exception e) {
            Log.warning(() -> "Cache deletion error: " + e.getMessage());
        }
    }

    /**
     * A helper class to return the cache result along with a hit/miss indicator.
     */
    public static class CacheResult<T> {
        private final T data;
        private final boolean cacheHit;

        public CacheResult(T data, boolean cacheHit) {
            this.data = data;
            this.cacheHit = cacheHit;
        }

        public T getData() {
            return data;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }
    }
}