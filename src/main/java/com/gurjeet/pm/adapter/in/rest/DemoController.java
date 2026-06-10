package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.adapter.out.notification.ExternalNotificationClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {
    private final StringRedisTemplate redis;

    public DemoController(StringRedisTemplate redis) { this.redis = redis; }

    @PostMapping("/notification-outage")
    public Map<String, Object> toggleOutage(@RequestParam boolean enabled) {
        redis.opsForValue().set(ExternalNotificationClient.OUTAGE_FLAG, String.valueOf(enabled));
        return Map.of("notificationOutage", enabled,
                "hint", enabled ? "Deliveries will now fail; watch the breaker open after 5 failures."
                                : "Outage cleared; queued notifications will drain.");
    }
}
