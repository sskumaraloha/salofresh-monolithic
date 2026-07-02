-- ============================================================
-- SaloFresh - Baseline schema
-- ============================================================

-- ---------- Location ----------
CREATE TABLE countries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    iso_code        VARCHAR(5)   NOT NULL,
    dial_code       VARCHAR(10),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_countries_name UNIQUE (name),
    CONSTRAINT uk_countries_iso_code UNIQUE (iso_code)
) ENGINE=InnoDB;

CREATE TABLE states (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    country_id      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_states_country FOREIGN KEY (country_id) REFERENCES countries (id)
) ENGINE=InnoDB;
CREATE INDEX idx_states_country ON states (country_id);

CREATE TABLE cities (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    state_id        BIGINT NOT NULL,
    is_popular      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_cities_state FOREIGN KEY (state_id) REFERENCES states (id)
) ENGINE=InnoDB;
CREATE INDEX idx_cities_state ON cities (state_id);
CREATE INDEX idx_cities_name ON cities (name);

-- ---------- Identity & Access ----------
CREATE TABLE permissions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_permissions_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB;

CREATE TABLE users (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                   VARCHAR(150) NOT NULL,
    phone                   VARCHAR(15),
    password                VARCHAR(255),
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100),
    profile_image_url       VARCHAR(500),
    gender                  VARCHAR(20),
    date_of_birth           DATE,
    auth_provider           VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id             VARCHAR(100),
    email_verified          BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified          BOOLEAN NOT NULL DEFAULT FALSE,
    account_status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    account_locked_until    TIMESTAMP NULL,
    last_login_at           TIMESTAMP NULL,
    referral_code           VARCHAR(20),
    referred_by_code        VARCHAR(20),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT uk_users_referral_code UNIQUE (referral_code)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(512) NOT NULL,
    user_id         BIGINT NOT NULL,
    expiry_date     TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    device_info     VARCHAR(255),
    ip_address      VARCHAR(64),
    remember_me     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE blacklisted_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id        VARCHAR(100) NOT NULL,
    expiry_date     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_blacklisted_tokens_token_id UNIQUE (token_id)
) ENGINE=InnoDB;

CREATE TABLE otp (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    identifier      VARCHAR(150) NOT NULL,
    otp_code        VARCHAR(255) NOT NULL,
    otp_type        VARCHAR(30) NOT NULL,
    otp_channel     VARCHAR(20) NOT NULL,
    expiry_date     TIMESTAMP NOT NULL,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    attempts        INT NOT NULL DEFAULT 0,
    last_sent_at    TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;
CREATE INDEX idx_otp_identifier_type ON otp (identifier, otp_type);

CREATE TABLE login_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    login_at        TIMESTAMP NOT NULL,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(500),
    success         BOOLEAN NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_login_history_user ON login_history (user_id);

CREATE TABLE addresses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    address_type    VARCHAR(20) NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    landmark        VARCHAR(150),
    city_id         BIGINT,
    pin_code        VARCHAR(10),
    latitude        DOUBLE,
    longitude       DOUBLE,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_addresses_city FOREIGN KEY (city_id) REFERENCES cities (id)
) ENGINE=InnoDB;
CREATE INDEX idx_addresses_user ON addresses (user_id);

CREATE TABLE customers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    membership_level    VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    total_bookings      INT NOT NULL DEFAULT 0,
    total_spent         DECIMAL(12,2) NOT NULL DEFAULT 0,
    preferred_salon_id  BIGINT,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_customers_user UNIQUE (user_id),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE salon_owners (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    business_name       VARCHAR(150),
    gst_number          VARCHAR(20),
    pan_number          VARCHAR(20),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bio                 VARCHAR(1000),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_salon_owners_user UNIQUE (user_id),
    CONSTRAINT fk_salon_owners_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------- Salon ----------
CREATE TABLE salons (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id                    BIGINT NOT NULL,
    name                        VARCHAR(150) NOT NULL,
    slug                        VARCHAR(180) NOT NULL,
    description                 VARCHAR(2000),
    address_line1               VARCHAR(255) NOT NULL,
    address_line2               VARCHAR(255),
    city_id                     BIGINT NOT NULL,
    pin_code                    VARCHAR(10),
    latitude                    DOUBLE NOT NULL,
    longitude                   DOUBLE NOT NULL,
    google_map_url              VARCHAR(500),
    contact_number              VARCHAR(15) NOT NULL,
    whatsapp_number             VARCHAR(15),
    email                       VARCHAR(150),
    website                     VARCHAR(255),
    opening_time                TIME NOT NULL,
    closing_time                TIME NOT NULL,
    parking_available           BOOLEAN NOT NULL DEFAULT FALSE,
    wifi_available              BOOLEAN NOT NULL DEFAULT FALSE,
    ac_available                BOOLEAN NOT NULL DEFAULT FALSE,
    waiting_area                BOOLEAN NOT NULL DEFAULT FALSE,
    kids_friendly               BOOLEAN NOT NULL DEFAULT FALSE,
    wheelchair_access           BOOLEAN NOT NULL DEFAULT FALSE,
    gender_type                 VARCHAR(20) NOT NULL,
    banner_image_url            VARCHAR(500),
    gst_number                  VARCHAR(20),
    business_license_number     VARCHAR(50),
    verification_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rating_average              DOUBLE NOT NULL DEFAULT 0,
    review_count                INT NOT NULL DEFAULT 0,
    slot_duration_minutes       INT NOT NULL DEFAULT 30,
    buffer_time_minutes         INT NOT NULL DEFAULT 5,
    max_bookings_per_slot       INT NOT NULL DEFAULT 1,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_salons_slug UNIQUE (slug),
    CONSTRAINT fk_salons_owner FOREIGN KEY (owner_id) REFERENCES salon_owners (id),
    CONSTRAINT fk_salons_city FOREIGN KEY (city_id) REFERENCES cities (id)
) ENGINE=InnoDB;
CREATE INDEX idx_salons_owner ON salons (owner_id);
CREATE INDEX idx_salons_city ON salons (city_id);
CREATE INDEX idx_salons_status ON salons (status);

CREATE TABLE working_hours (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT NOT NULL,
    day_of_week         VARCHAR(15) NOT NULL,
    is_open             BOOLEAN NOT NULL DEFAULT TRUE,
    start_time          TIME,
    end_time            TIME,
    break_start_time    TIME,
    break_end_time      TIME,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_working_hours_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_working_hours_salon ON working_hours (salon_id);

CREATE TABLE holidays (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT NOT NULL,
    holiday_date        DATE NOT NULL,
    reason              VARCHAR(255),
    recurring_yearly    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_holidays_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_holidays_salon ON holidays (salon_id);

CREATE TABLE gallery (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id        BIGINT NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    caption         VARCHAR(255),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_gallery_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_gallery_salon ON gallery (salon_id);

CREATE TABLE documents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id        BIGINT NOT NULL,
    document_type   VARCHAR(30) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_documents_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_documents_salon ON documents (salon_id);

-- ---------- Category & Services ----------
CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    description     VARCHAR(1000),
    image_url       VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_categories_type UNIQUE (type)
) ENGINE=InnoDB;

CREATE TABLE services (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT NOT NULL,
    category_id         BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(2000),
    duration_minutes    INT NOT NULL,
    price               DECIMAL(10,2) NOT NULL,
    discount_price      DECIMAL(10,2),
    tax_percentage      DECIMAL(5,2) NOT NULL DEFAULT 18.0,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    image_url           VARCHAR(500),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_services_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;
CREATE INDEX idx_services_salon ON services (salon_id);
CREATE INDEX idx_services_category ON services (category_id);

-- ---------- Employees ----------
CREATE TABLE employees (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    salon_id            BIGINT NOT NULL,
    user_id             BIGINT,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100),
    email               VARCHAR(150),
    phone               VARCHAR(15),
    designation         VARCHAR(100),
    joining_date        DATE,
    salary              DECIMAL(12,2),
    employment_status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    profile_image_url   VARCHAR(500),
    rating_average      DOUBLE NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_employees_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_employees_salon ON employees (salon_id);

CREATE TABLE employee_services (
    employee_id     BIGINT NOT NULL,
    service_id      BIGINT NOT NULL,
    PRIMARY KEY (employee_id, service_id),
    CONSTRAINT fk_employee_services_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_employee_services_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB;

CREATE TABLE employee_schedule (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id         BIGINT NOT NULL,
    day_of_week         VARCHAR(15) NOT NULL,
    is_working_day      BOOLEAN NOT NULL DEFAULT TRUE,
    start_time          TIME,
    end_time            TIME,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_employee_schedule_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;
CREATE INDEX idx_employee_schedule_employee ON employee_schedule (employee_id);

CREATE TABLE employee_leave (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_employee_leave_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;
CREATE INDEX idx_employee_leave_employee ON employee_leave (employee_id);

CREATE TABLE employee_attendance (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id         BIGINT NOT NULL,
    attendance_date     DATE NOT NULL,
    status              VARCHAR(20) NOT NULL,
    check_in            TIMESTAMP NULL,
    check_out           TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_employee_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;
CREATE INDEX idx_employee_attendance_employee ON employee_attendance (employee_id);

-- ---------- Appointments ----------
CREATE TABLE appointments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_number      VARCHAR(40) NOT NULL,
    customer_id         BIGINT NOT NULL,
    salon_id            BIGINT NOT NULL,
    employee_id         BIGINT,
    appointment_date    DATE NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount        DECIMAL(12,2) NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(12,2) NOT NULL DEFAULT 0,
    final_amount        DECIMAL(12,2) NOT NULL,
    coupon_code         VARCHAR(30),
    notes               VARCHAR(1000),
    cancelled_reason    VARCHAR(500),
    cancelled_at        TIMESTAMP NULL,
    completed_at        TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_appointments_booking_number UNIQUE (booking_number),
    CONSTRAINT fk_appointments_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_appointments_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;
CREATE INDEX idx_appointments_customer ON appointments (customer_id);
CREATE INDEX idx_appointments_salon_date ON appointments (salon_id, appointment_date);
CREATE INDEX idx_appointments_employee_date ON appointments (employee_id, appointment_date);

CREATE TABLE appointment_services (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id      BIGINT NOT NULL,
    service_id          BIGINT NOT NULL,
    service_name        VARCHAR(150) NOT NULL,
    price               DECIMAL(10,2) NOT NULL,
    duration_minutes    INT NOT NULL,
    quantity            INT NOT NULL DEFAULT 1,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_appointment_services_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_appointment_services_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB;
CREATE INDEX idx_appointment_services_appointment ON appointment_services (appointment_id);

-- ---------- Payments ----------
CREATE TABLE payments (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id              BIGINT,
    user_id                     BIGINT NOT NULL,
    amount                      DECIMAL(12,2) NOT NULL,
    refunded_amount             DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency                    VARCHAR(5) NOT NULL DEFAULT 'INR',
    payment_method              VARCHAR(20) NOT NULL,
    payment_status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_order_id            VARCHAR(100),
    gateway_transaction_id      VARCHAR(100),
    gateway_signature           VARCHAR(255),
    invoice_number              VARCHAR(40),
    failure_reason              VARCHAR(500),
    paid_at                     TIMESTAMP NULL,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NULL,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_payments_invoice_number UNIQUE (invoice_number),
    CONSTRAINT fk_payments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_payments_user ON payments (user_id);

CREATE TABLE refunds (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id          BIGINT NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_refund_id   VARCHAR(100),
    processed_at        TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_refunds_payment ON refunds (payment_id);

CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number    VARCHAR(40) NOT NULL,
    user_id             BIGINT NOT NULL,
    payment_id          BIGINT,
    transaction_type    VARCHAR(10) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    reference_type      VARCHAR(30),
    description         VARCHAR(500),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_transactions_reference_number UNIQUE (reference_number),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_transactions_user ON transactions (user_id);

CREATE TABLE wallet (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    balance         DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency        VARCHAR(5) NOT NULL DEFAULT 'INR',
    version         BIGINT,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_wallet_user UNIQUE (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE wallet_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       BIGINT NOT NULL,
    type            VARCHAR(10) NOT NULL,
    source          VARCHAR(30) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    balance_after   DECIMAL(12,2) NOT NULL,
    reference_id    VARCHAR(50),
    description     VARCHAR(500),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
) ENGINE=InnoDB;
CREATE INDEX idx_wallet_transactions_wallet ON wallet_transactions (wallet_id);

CREATE TABLE reward_points (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(20) NOT NULL,
    points          INT NOT NULL,
    balance_after   INT NOT NULL,
    reference_id    VARCHAR(50),
    description     VARCHAR(500),
    expiry_date     DATE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reward_points_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_reward_points_user ON reward_points (user_id);

-- ---------- Reviews ----------
CREATE TABLE reviews (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id         BIGINT NOT NULL,
    salon_id            BIGINT NOT NULL,
    employee_id         BIGINT,
    appointment_id      BIGINT,
    salon_rating        INT NOT NULL,
    employee_rating     INT,
    comment             VARCHAR(2000),
    owner_reply         VARCHAR(2000),
    owner_reply_at      TIMESTAMP NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    report_reason       VARCHAR(500),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_salon FOREIGN KEY (salon_id) REFERENCES salons (id),
    CONSTRAINT fk_reviews_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_reviews_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_reviews_salon ON reviews (salon_id);
CREATE INDEX idx_reviews_customer ON reviews (customer_id);

CREATE TABLE review_images (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id       BIGINT NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id)
) ENGINE=InnoDB;
CREATE INDEX idx_review_images_review ON review_images (review_id);

-- ---------- Favorites ----------
CREATE TABLE favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    salon_id        BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_favorite_user_salon UNIQUE (user_id, salon_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_favorites_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;

CREATE TABLE recently_viewed_salons (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    salon_id        BIGINT NOT NULL,
    viewed_at       TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_recently_viewed_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recently_viewed_salon FOREIGN KEY (salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;
CREATE INDEX idx_recently_viewed_user ON recently_viewed_salons (user_id);

-- ---------- Coupons ----------
CREATE TABLE coupons (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                    VARCHAR(30) NOT NULL,
    type                    VARCHAR(20) NOT NULL,
    discount_value          DECIMAL(10,2) NOT NULL,
    max_discount_amount     DECIMAL(10,2),
    min_order_amount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    usage_limit             INT,
    usage_per_user          INT DEFAULT 1,
    times_used              INT NOT NULL DEFAULT 0,
    valid_from              TIMESTAMP NOT NULL,
    valid_to                TIMESTAMP NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    description             VARCHAR(500),
    applicable_salon_id     BIGINT,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT fk_coupons_salon FOREIGN KEY (applicable_salon_id) REFERENCES salons (id)
) ENGINE=InnoDB;

CREATE TABLE coupon_usage (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id           BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    appointment_id      BIGINT,
    used_at             TIMESTAMP NOT NULL,
    discount_amount     DECIMAL(10,2) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_coupon_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_coupon_usage_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
) ENGINE=InnoDB;
CREATE INDEX idx_coupon_usage_coupon_user ON coupon_usage (coupon_id, user_id);

-- ---------- Notifications & Logs ----------
CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(40) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         VARCHAR(2000) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id    VARCHAR(50),
    reference_type  VARCHAR(30),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_notifications_user ON notifications (user_id);

CREATE TABLE email_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient       VARCHAR(150) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            LONGTEXT,
    status          VARCHAR(20) NOT NULL,
    sent_at         TIMESTAMP NULL,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE sms_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient       VARCHAR(15) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    sent_at         TIMESTAMP NULL,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(50) NOT NULL,
    action          VARCHAR(20) NOT NULL,
    performed_by    VARCHAR(100),
    old_value       LONGTEXT,
    new_value       LONGTEXT,
    performed_at    TIMESTAMP NOT NULL
) ENGINE=InnoDB;
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
