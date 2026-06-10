package com.gurjeet.pm.domain.port;

import java.util.UUID;

public interface BoardBroadcastPort {
    void broadcast(UUID projectId, String jsonMessage);
}
