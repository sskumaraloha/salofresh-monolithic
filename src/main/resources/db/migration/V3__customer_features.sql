-- ============================================================
-- SaloFresh - Customer-experience feature set:
-- push notifications, WhatsApp, waitlist, memberships, gift cards,
-- family/dependent profiles, in-app chat, multi-criteria ratings,
-- favorite stylist, guest booking support.
-- ============================================================

-- ---------- Favorite stylist ----------
ALTER TABLE customers ADD COLUMN preferred_employee_id BIGINT NULL;
ALTER TABLE customers ADD CONSTRAINT fk_customers_preferred_employee
    FOREIGN KEY (preferred_employee_id) REFERENCES employees (id);

-- ---------- Multi-criteria ratings ----------
ALTER TABLE reviews ADD COLUMN cleanliness_rating INT NULL;
ALTER TABLE reviews ADD COLUMN service_quality_rating INT NULL;
ALTER TABLE reviews ADD COLUMN value_for_money_rating INT NULL;

-- ---------- Device tokens (push notifications) ----------
CREATE TABLE device_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token           VARCHAR(500) NOT NULL,
    platform        VARCHAR(20) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);

-- ---------- WhatsApp logs ----------
CREATE TABLE whatsapp_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient       VARCHAR(15) NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    sent_at         TIMESTAMP NULL,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

-- ---------- Waitlist ----------
CREATE TABLE waitlist (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    salon_id                BIGINT NOT NULL,
    employee_id             BIGINT,
    service_id              BIGINT,
    preferred_date          DATE NOT NULL,
    preferred_start_time    TIME,
    preferred_end_time      TIME,
    status                  VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    notified_at             TIMESTAMP NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_waitlist_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_waitlist_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_waitlist_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_waitlist_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB;
CREATE INDEX idx_waitlist_salon_date ON waitlist (salon_id, preferred_date, status);
CREATE INDEX idx_waitlist_employee_date ON waitlist (employee_id, preferred_date, status);
CREATE INDEX idx_waitlist_user ON waitlist (user_id);

-- ---------- Membership plans & subscriptions ----------
CREATE TABLE membership_plans (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(1000),
    price               DECIMAL(10,2) NOT NULL,
    validity_days       INT NOT NULL,
    total_sessions      INT,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_membership_plans_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_membership_plans_salon ON membership_plans (salon_id);

CREATE TABLE membership_subscriptions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    plan_id             BIGINT NOT NULL,
    payment_id          BIGINT,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    remaining_sessions  INT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_membership_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_membership_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id),
    CONSTRAINT fk_membership_subscriptions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_membership_subscriptions_user ON membership_subscriptions (user_id);

-- ---------- Gift cards ----------
CREATE TABLE gift_cards (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(30) NOT NULL,
    initial_amount      DECIMAL(10,2) NOT NULL,
    balance             DECIMAL(10,2) NOT NULL,
    purchased_by_id     BIGINT NOT NULL,
    recipient_name      VARCHAR(150),
    recipient_email     VARCHAR(150),
    recipient_phone     VARCHAR(15),
    message             VARCHAR(500),
    issued_at           TIMESTAMP NOT NULL,
    expiry_date         DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    redeemed_by_id      BIGINT,
    redeemed_at         TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_gift_cards_code UNIQUE (code),
    CONSTRAINT fk_gift_cards_purchased_by FOREIGN KEY (purchased_by_id) REFERENCES users (id),
    CONSTRAINT fk_gift_cards_redeemed_by FOREIGN KEY (redeemed_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_gift_cards_purchased_by ON gift_cards (purchased_by_id);

-- ---------- Family / dependent profiles ----------
CREATE TABLE family_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    relationship    VARCHAR(20) NOT NULL,
    gender          VARCHAR(20),
    date_of_birth   DATE,
    phone           VARCHAR(15),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_family_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_family_members_user ON family_members (user_id);

-- ---------- Appointment links: family member + membership subscription ----------
ALTER TABLE appointments ADD COLUMN family_member_id BIGINT NULL;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_family_member
    FOREIGN KEY (family_member_id) REFERENCES family_members (id);

ALTER TABLE appointments ADD COLUMN membership_subscription_id BIGINT NULL;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_membership_subscription
    FOREIGN KEY (membership_subscription_id) REFERENCES membership_subscriptions (id);

-- ---------- In-app chat ----------
CREATE TABLE chat_conversations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id         BIGINT NOT NULL,
    salon_id            BIGINT,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    last_message_at     TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_chat_conversations_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_chat_conversations_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_chat_conversations_customer ON chat_conversations (customer_id);
CREATE INDEX idx_chat_conversations_salon ON chat_conversations (salon_id);

CREATE TABLE chat_messages (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id     BIGINT NOT NULL,
    sender_id           BIGINT NOT NULL,
    message             VARCHAR(2000) NOT NULL,
    sent_at             TIMESTAMP NOT NULL,
    read_at             TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_chat_messages_conversation ON chat_messages (conversation_id, sent_at);
