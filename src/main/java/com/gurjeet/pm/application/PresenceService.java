package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.UserRepository;
import com.gurjeet.pm.domain.model.UserEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class PresenceService {
    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    public PresenceService(StringRedisTemplate redis, UserRepository userRepository) {
        this.redis = redis;
        this.userRepository = userRepository;
    }

    private String boardKey(UUID projectId) { return "presence:board:" + projectId; }
    private String issueKey(UUID issueId) { return "presence:issue:" + issueId; }

    public void join(UUID projectId, UUID issueId, UUID userId) {
        redis.opsForSet().add(boardKey(projectId), userId.toString());
        redis.expire(boardKey(projectId), Duration.ofHours(12));
        if (issueId != null) {
            redis.opsForSet().add(issueKey(issueId), userId.toString());
            redis.expire(issueKey(issueId), Duration.ofHours(12));
        }
    }

    public void leave(UUID projectId, UUID issueId, UUID userId) {
        redis.opsForSet().remove(boardKey(projectId), userId.toString());
        if (issueId != null) redis.opsForSet().remove(issueKey(issueId), userId.toString());
    }

    public List<Map<String, String>> viewers(UUID projectId) {
        Set<String> ids = redis.opsForSet().members(boardKey(projectId));
        if (ids == null || ids.isEmpty()) return List.of();
        List<UUID> uuids = ids.stream().map(UUID::fromString).toList();
        List<Map<String, String>> viewers = new ArrayList<>();
        for (UserEntity user : userRepository.findAllById(uuids)) {
            viewers.add(Map.of("id", user.getId().toString(), "name", user.getDisplayName()));
        }
        return viewers;
    }
}
