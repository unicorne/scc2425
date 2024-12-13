package tukano.impl;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import utils.ResourceUtils;

import java.util.Base64;
import java.util.Properties;

public class RedisCachePool {

    private static JedisPool instance;

    // Synchronized method to get the Redis client pool (JedisPool)
    public synchronized static JedisPool getCachePool() {
        if (instance != null) {
            return instance;
        }
        Properties propsRedis = new Properties();
        ResourceUtils.loadPropertiesFromResources(propsRedis, "redis.properties");

        String RedisHostname = propsRedis.getProperty("redisHostName");
        String RedisKeyB64 = propsRedis.getProperty("redisKey");
        String RedisKey = new String(Base64.getDecoder().decode(RedisKeyB64));
        int REDIS_PORT = Integer.parseInt(propsRedis.getProperty("redisPort", "6380"));
        int REDIS_TIMEOUT = Integer.parseInt(propsRedis.getProperty("redisTimeout", "1000"));
        boolean REDIS_USE_TLS = Boolean.parseBoolean(propsRedis.getProperty("redisUseTls", "true"));

        // Create the Jedis pool configuration
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128); // Max number of total connections
        poolConfig.setMaxIdle(128);  // Max number of idle connections
        poolConfig.setMinIdle(16);   // Min number of idle connections
        poolConfig.setBlockWhenExhausted(true); // Block when the pool is exhausted

        // Create the JedisPool with TLS and authentication
        instance = new JedisPool(poolConfig, RedisHostname, REDIS_PORT, REDIS_TIMEOUT, RedisKey, REDIS_USE_TLS);

        return instance;
    }
}

