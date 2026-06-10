package com.gurjeet.pm.domain.port;

import com.gurjeet.pm.domain.model.Notification;

public interface NotificationPort {
    void deliver(Notification notification);
}
