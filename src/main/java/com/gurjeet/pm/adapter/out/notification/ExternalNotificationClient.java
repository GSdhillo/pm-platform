package com.gurjeet.pm.adapter.out.notification;

import com.gurjeet.pm.domain.model.Notification;
import com.gurjeet.pm.domain.port.NotificationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExternalNotificationClient implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(ExternalNotificationClient.class);
    public static final String OUTAGE_FLAG = "demo:notification-outage";

    private final StringRedisTemplate redis;

    public ExternalNotificationClient(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    @CircuitBreaker(name = "notificationService")
    public void deliver(Notification notification) {
        if ("true".equals(redis.opsForValue().get(OUTAGE_FLAG))) {
            throw new IllegalStateException("Simulated notification service outage");
        }

        log.info("Delivered notification {} type={} to user={}",
                notification.getId(), notification.getType(), notification.getUserId());
    }
}
