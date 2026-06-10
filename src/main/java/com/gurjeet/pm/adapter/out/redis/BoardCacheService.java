package com.gurjeet.pm.adapter.out.redis;

import com.gurjeet.pm.domain.port.BoardCachePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class BoardCacheService implements BoardCachePort {
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public BoardCacheService(StringRedisTemplate redis,
                             @Value("${app.board-cache.ttl-seconds:30}") long ttlSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    private String key(UUID projectId) { return "board:" + projectId; }

    @Override
    public Optional<String> get(UUID projectId) {
        return Optional.ofNullable(redis.opsForValue().get(key(projectId)));
    }

    @Override
    public void put(UUID projectId, String boardJson) {
        redis.opsForValue().set(key(projectId), boardJson, ttl);
    }

    @Override
    public void evict(UUID projectId) {
        redis.delete(key(projectId));
    }
}
