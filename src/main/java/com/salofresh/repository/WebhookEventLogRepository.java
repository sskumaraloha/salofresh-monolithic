package com.salofresh.repository;

import com.salofresh.entity.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    Optional<WebhookEventLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
