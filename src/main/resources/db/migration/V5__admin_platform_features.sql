-- ============================================================
-- SaloFresh - Admin / platform feature set:
-- support ticketing, fraud/anomaly alerts, feature flags,
-- GDPR/DPDP data subject requests, payment webhook event log,
-- and a maker-checker approval workflow for high-risk actions.
-- ============================================================

-- ---------- Support ticketing ----------
CREATE TABLE support_tickets (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by_user_id      BIGINT NOT NULL,
    subject                 VARCHAR(200) NOT NULL,
    category                VARCHAR(20) NOT NULL,
    priority                VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to_id          BIGINT,
    resolved_at             TIMESTAMP NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_support_tickets_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_support_tickets_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_support_tickets_created_by ON support_tickets (created_by_user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);

CREATE TABLE ticket_messages (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id           BIGINT NOT NULL,
    sender_id           BIGINT NOT NULL,
    message             VARCHAR(2000) NOT NULL,
    is_internal_note    BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at             TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id),
    CONSTRAINT fk_ticket_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_ticket_messages_ticket ON ticket_messages (ticket_id, sent_at);

-- ---------- Fraud / anomaly detection ----------
CREATE TABLE fraud_alerts (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    type                    VARCHAR(30) NOT NULL,
    related_user_id         BIGINT,
    related_entity_type     VARCHAR(30),
    related_entity_id       VARCHAR(50),
    description             VARCHAR(1000) NOT NULL,
    severity                VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    detected_at             TIMESTAMP NOT NULL,
    resolved_by_id          BIGINT,
    resolved_at             TIMESTAMP NULL,
    resolution_note         VARCHAR(1000),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_fraud_alerts_related_user FOREIGN KEY (related_user_id) REFERENCES users (id),
    CONSTRAINT fk_fraud_alerts_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts (status, detected_at);

-- ---------- Feature flags / dynamic config ----------
CREATE TABLE feature_flags (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_key        VARCHAR(100) NOT NULL,
    flag_value      VARCHAR(500) NOT NULL,
    data_type       VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_feature_flags_key UNIQUE (flag_key)
) ENGINE=InnoDB;

-- ---------- GDPR / DPDP data subject requests ----------
CREATE TABLE data_subject_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    request_type        VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at        TIMESTAMP NOT NULL,
    completed_at        TIMESTAMP NULL,
    export_file_url     VARCHAR(500),
    rejection_reason    VARCHAR(500),
    processed_by_id      BIGINT,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_data_subject_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_data_subject_requests_processed_by FOREIGN KEY (processed_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_data_subject_requests_user ON data_subject_requests (user_id);
CREATE INDEX idx_data_subject_requests_status ON data_subject_requests (status);

-- ---------- Payment webhook event log ----------
CREATE TABLE webhook_event_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(20) NOT NULL,
    event_id        VARCHAR(150) NOT NULL,
    event_type      VARCHAR(100),
    payload         LONGTEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at     TIMESTAMP NOT NULL,
    processed_at    TIMESTAMP NULL,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_webhook_event_logs_event_id UNIQUE (event_id)
) ENGINE=InnoDB;

-- ---------- Maker-checker approval workflow ----------
CREATE TABLE approval_requests (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type             VARCHAR(30) NOT NULL,
    requested_by_id         BIGINT NOT NULL,
    payload                 LONGTEXT NOT NULL,
    related_entity_type     VARCHAR(30),
    related_entity_id       VARCHAR(50),
    reason                  VARCHAR(500),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by_id           BIGINT,
    decided_at              TIMESTAMP NULL,
    rejection_reason        VARCHAR(500),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_approval_requests_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id),
    CONSTRAINT fk_approval_requests_decided_by FOREIGN KEY (decided_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_approval_requests_status ON approval_requests (status);
