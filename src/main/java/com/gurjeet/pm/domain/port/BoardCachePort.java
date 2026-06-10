package com.gurjeet.pm.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface BoardCachePort {
    Optional<String> get(UUID projectId);
    void put(UUID projectId, String boardJson);
    void evict(UUID projectId);
}
