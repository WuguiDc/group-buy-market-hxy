package com.hxy.infrastructure.adapter.repository;

import com.hxy.infrastructure.dcc.DccService;
import com.hxy.infrastructure.redis.IRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.function.Supplier;

public abstract class AbstractRepository {

    private final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);

    @Resource
    protected IRedisService redisService;

    @Resource
    protected DccService dccService;

    /** 优先读取缓存，未命中时查询数据库并缓存非空结果。 */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback) {
        if (!dccService.isCacheOpenService()) {
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }

        T cacheResult = redisService.getValue(cacheKey);
        if (cacheResult != null) {
            return cacheResult;
        }

        T dbResult = dbFallback.get();
        if (dbResult == null) {
            return null;
        }
        redisService.setValue(cacheKey, dbResult);
        return dbResult;
    }

    /**
     * 优先读取缓存，未命中时查询数据库并缓存非空结果。
     *
     * @param expired 缓存过期时间，单位：毫秒
     */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback, long expired) {
        if (!dccService.isCacheOpenService()) {
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }

        T cacheResult = redisService.getValue(cacheKey);
        if (cacheResult != null) {
            return cacheResult;
        }

        T dbResult = dbFallback.get();
        if (dbResult == null) {
            return null;
        }
        redisService.setValue(cacheKey, dbResult, expired);
        return dbResult;
    }

}
