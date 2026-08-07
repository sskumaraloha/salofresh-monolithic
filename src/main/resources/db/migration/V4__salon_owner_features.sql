-- ============================================================
-- SaloFresh - Salon-owner feature set:
-- staff commission/payroll, inventory management, CRM notes,
-- marketing campaigns, platform billing/subscriptions,
-- shift-swap requests, salon payout/settlement reports.
-- ============================================================

-- ---------- Commission & payroll ----------
CREATE TABLE commission_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id        BIGINT NOT NULL,
    employee_id     BIGINT,
    category_id     BIGINT,
    rule_type       VARCHAR(20) NOT NULL,
    rule_value      DECIMAL(10,2) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_commission_rules_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_commission_rules_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_commission_rules_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;
CREATE INDEX idx_commission_rules_salon ON commission_rules (salon_id);
CREATE INDEX idx_commission_rules_employee ON commission_rules (employee_id);

CREATE TABLE payroll_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id         BIGINT NOT NULL,
    salon_id            BIGINT NOT NULL,
    period_start        DATE NOT NULL,
    period_end          DATE NOT NULL,
    base_salary         DECIMAL(12,2) NOT NULL DEFAULT 0,
    commission_earned   DECIMAL(12,2) NOT NULL DEFAULT 0,
    deductions          DECIMAL(12,2) NOT NULL DEFAULT 0,
    bonus               DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_pay             DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    paid_at             TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payroll_records_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_payroll_records_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_payroll_records_salon ON payroll_records (salon_id, period_start);
CREATE INDEX idx_payroll_records_employee ON payroll_records (employee_id, period_start);

-- ---------- Inventory ----------
CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id        BIGINT NOT NULL,
    name            VARCHAR(150) NOT NULL,
    sku             VARCHAR(50),
    unit            VARCHAR(20),
    current_stock   INT NOT NULL DEFAULT 0,
    reorder_level   INT NOT NULL DEFAULT 0,
    cost_price      DECIMAL(10,2),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_products_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_products_salon ON products (salon_id);

CREATE TABLE stock_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT NOT NULL,
    type                VARCHAR(20) NOT NULL,
    quantity            INT NOT NULL,
    reason              VARCHAR(255),
    reference_type      VARCHAR(30),
    reference_id        VARCHAR(50),
    performed_by_id     BIGINT,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_stock_transactions_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_transactions_performed_by FOREIGN KEY (performed_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_stock_transactions_product ON stock_transactions (product_id);

CREATE TABLE service_product_usage (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id              BIGINT NOT NULL,
    product_id              BIGINT NOT NULL,
    quantity_per_service    INT NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_service_product_usage_service FOREIGN KEY (service_id) REFERENCES services (id),
    CONSTRAINT fk_service_product_usage_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;
CREATE INDEX idx_service_product_usage_service ON service_product_usage (service_id);

-- ---------- CRM notes ----------
CREATE TABLE customer_notes (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id                BIGINT NOT NULL,
    customer_id             BIGINT NOT NULL,
    created_by_user_id      BIGINT NOT NULL,
    note                    VARCHAR(2000) NOT NULL,
    tag                     VARCHAR(20),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_customer_notes_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_customer_notes_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_customer_notes_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_customer_notes_salon_customer ON customer_notes (salon_id, customer_id);

-- ---------- Marketing campaigns ----------
CREATE TABLE marketing_campaigns (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id                BIGINT NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    message                 VARCHAR(2000) NOT NULL,
    channel                 VARCHAR(20) NOT NULL,
    target_audience         VARCHAR(20) NOT NULL,
    scheduled_at            TIMESTAMP NULL,
    sent_at                 TIMESTAMP NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id      BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_marketing_campaigns_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_marketing_campaigns_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_marketing_campaigns_salon ON marketing_campaigns (salon_id);

CREATE TABLE campaign_recipients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id     BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP NULL,
    error_message   VARCHAR(500),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_campaign_recipients_campaign FOREIGN KEY (campaign_id) REFERENCES marketing_campaigns (id),
    CONSTRAINT fk_campaign_recipients_customer FOREIGN KEY (customer_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_campaign_recipients_campaign ON campaign_recipients (campaign_id);

-- ---------- Platform billing (salon owner -> platform) ----------
CREATE TABLE platform_plans (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                        VARCHAR(150) NOT NULL,
    description                 VARCHAR(1000),
    price                       DECIMAL(10,2) NOT NULL,
    billing_cycle               VARCHAR(20) NOT NULL,
    max_salons                  INT,
    commission_percentage       DECIMAL(5,2) NOT NULL,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE platform_subscriptions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_owner_id      BIGINT NOT NULL,
    plan_id             BIGINT NOT NULL,
    payment_id          BIGINT,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_platform_subscriptions_owner FOREIGN KEY (salon_owner_id) REFERENCES salon_owners (id),
    CONSTRAINT fk_platform_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES platform_plans (id),
    CONSTRAINT fk_platform_subscriptions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_platform_subscriptions_owner ON platform_subscriptions (salon_owner_id);

-- ---------- Shift swap requests ----------
CREATE TABLE shift_swap_requests (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id                    BIGINT NOT NULL,
    requesting_employee_id      BIGINT NOT NULL,
    target_employee_id          BIGINT,
    shift_date                  DATE NOT NULL,
    original_start_time         TIME NOT NULL,
    original_end_time           TIME NOT NULL,
    reason                      VARCHAR(500),
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responded_by_id             BIGINT,
    responded_at                TIMESTAMP NULL,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_shift_swap_requests_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_shift_swap_requests_requesting_employee FOREIGN KEY (requesting_employee_id) REFERENCES employees (id),
    CONSTRAINT fk_shift_swap_requests_target_employee FOREIGN KEY (target_employee_id) REFERENCES employees (id),
    CONSTRAINT fk_shift_swap_requests_responded_by FOREIGN KEY (responded_by_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_shift_swap_requests_salon ON shift_swap_requests (salon_id, status);
CREATE INDEX idx_shift_swap_requests_requesting_employee ON shift_swap_requests (requesting_employee_id);

-- ---------- Salon payout / settlement reports ----------
CREATE TABLE salon_payouts (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id                        BIGINT NOT NULL,
    period_start                    DATE NOT NULL,
    period_end                      DATE NOT NULL,
    gross_revenue                   DECIMAL(12,2) NOT NULL,
    platform_commission_amount      DECIMAL(12,2) NOT NULL,
    net_payout_amount               DECIMAL(12,2) NOT NULL,
    status                          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payout_reference                VARCHAR(50),
    processed_at                    TIMESTAMP NULL,
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP NULL,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    is_deleted                      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_salon_payouts_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_salon_payouts_salon ON salon_payouts (salon_id, period_start);

-- ---------- Seed a default platform plan so the platform-billing feature is usable out of the box ----------
INSERT INTO platform_plans (name, description, price, billing_cycle, max_salons, commission_percentage, active,
                             created_at, created_by, is_deleted)
VALUES ('Starter', 'Single-salon plan with standard platform commission', 999.00, 'MONTHLY', 1, 10.00, TRUE,
        CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
       ('Growth', 'Up to 5 salons with a reduced commission rate', 2999.00, 'MONTHLY', 5, 7.50, TRUE,
        CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
       ('Enterprise', 'Unlimited salons with the lowest commission rate', 9999.00, 'MONTHLY', NULL, 5.00, TRUE,
        CURRENT_TIMESTAMP, 'SYSTEM', FALSE);
