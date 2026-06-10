package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.DomainEventRepository;
import com.gurjeet.pm.domain.model.DomainEvent;
import com.gurjeet.pm.domain.port.BoardBroadcastPort;
import com.gurjeet.pm.domain.port.BoardCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final DomainEventRepository eventRepository;
    private final BoardBroadcastPort broadcastPort;
    private final BoardCachePort boardCache;
    private final NotificationService notificationService;
    private final EventMessageMapper messageMapper;

    public OutboxDispatcher(DomainEventRepository eventRepository, BoardBroadcastPort broadcastPort,
                            BoardCachePort boardCache, NotificationService notificationService,
                            EventMessageMapper messageMapper) {
        this.eventRepository = eventRepository;
        this.broadcastPort = broadcastPort;
        this.boardCache = boardCache;
        this.notificationService = notificationService;
        this.messageMapper = messageMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:200}")
    @Transactional
    public void dispatch() {
        List<DomainEvent> batch = eventRepository.findTop100ByDispatchedAtIsNullOrderByIdAsc();
        for (DomainEvent event : batch) {
            try {
                broadcastPort.broadcast(event.getProjectId(), messageMapper.toWsMessage(event));
                notificationService.fanout(event);
                boardCache.evict(event.getProjectId());
                event.setDispatchedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to dispatch event {} ({})", event.getId(), event.getEventType(), e);
                return;
            }
        }
    }
}
