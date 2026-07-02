-- ============================================================
-- SaloFresh - Reference / seed data
-- ============================================================

INSERT INTO roles (name, description, created_at, created_by, is_deleted) VALUES
    ('SUPER_ADMIN', 'Full platform control', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('ADMIN', 'Platform administrator', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('SALON_OWNER', 'Owns and manages one or more salons', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('EMPLOYEE', 'Salon staff member', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('CUSTOMER', 'End customer booking appointments', CURRENT_TIMESTAMP, 'SYSTEM', FALSE);

INSERT INTO permissions (name, description, created_at, created_by, is_deleted) VALUES
    ('MANAGE_PLATFORM', 'Manage entire platform configuration', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('MANAGE_USERS', 'Manage user accounts', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('MANAGE_SALONS', 'Approve, reject and manage salons', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('MANAGE_OWN_SALON', 'Manage salons owned by the current user', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('MANAGE_BOOKINGS', 'Manage appointment bookings', CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('VIEW_REPORTS', 'View analytics and reports', CURRENT_TIMESTAMP, 'SYSTEM', FALSE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('MANAGE_USERS', 'MANAGE_SALONS', 'MANAGE_BOOKINGS', 'VIEW_REPORTS');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SALON_OWNER' AND p.name IN ('MANAGE_OWN_SALON', 'MANAGE_BOOKINGS', 'VIEW_REPORTS');

INSERT INTO categories (name, type, description, active, created_at, created_by, is_deleted) VALUES
    ('Hair Cut', 'HAIR_CUT', 'Professional hair cutting services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Hair Spa', 'HAIR_SPA', 'Relaxing and nourishing hair spa treatments', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Hair Color', 'HAIR_COLOR', 'Hair coloring and highlighting services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Hair Wash', 'HAIR_WASH', 'Shampoo and hair wash services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Shaving', 'SHAVING', 'Traditional and modern shaving services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Facial', 'FACIAL', 'Skin rejuvenating facial treatments', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Bleach', 'BLEACH', 'Skin bleaching services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Waxing', 'WAXING', 'Full and partial body waxing', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Threading', 'THREADING', 'Eyebrow and facial threading', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Massage', 'MASSAGE', 'Therapeutic and relaxation massages', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Body Spa', 'BODY_SPA', 'Full body spa treatments', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Bridal Makeup', 'BRIDAL_MAKEUP', 'Complete bridal makeup packages', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Nail Art', 'NAIL_ART', 'Creative nail art designs', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Pedicure', 'PEDICURE', 'Foot care and pedicure services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Manicure', 'MANICURE', 'Hand care and manicure services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Tattoo', 'TATTOO', 'Custom tattoo art services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Skin Treatment', 'SKIN_TREATMENT', 'Advanced skin care treatments', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE),
    ('Custom Services', 'CUSTOM', 'Other custom salon services', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE);

INSERT INTO countries (name, iso_code, dial_code, created_at, created_by, is_deleted) VALUES
    ('India', 'IN', '+91', CURRENT_TIMESTAMP, 'SYSTEM', FALSE);

INSERT INTO states (name, country_id, created_at, created_by, is_deleted)
SELECT 'Maharashtra', id, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM countries WHERE iso_code = 'IN';
INSERT INTO states (name, country_id, created_at, created_by, is_deleted)
SELECT 'Karnataka', id, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM countries WHERE iso_code = 'IN';
INSERT INTO states (name, country_id, created_at, created_by, is_deleted)
SELECT 'Delhi', id, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM countries WHERE iso_code = 'IN';
INSERT INTO states (name, country_id, created_at, created_by, is_deleted)
SELECT 'Telangana', id, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM countries WHERE iso_code = 'IN';
INSERT INTO states (name, country_id, created_at, created_by, is_deleted)
SELECT 'Tamil Nadu', id, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM countries WHERE iso_code = 'IN';

INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Mumbai', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Maharashtra';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Pune', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Maharashtra';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Bengaluru', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Karnataka';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Mysuru', id, FALSE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Karnataka';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'New Delhi', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Delhi';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Hyderabad', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Telangana';
INSERT INTO cities (name, state_id, is_popular, created_at, created_by, is_deleted)
SELECT 'Chennai', id, TRUE, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM states WHERE name = 'Tamil Nadu';

-- Default SUPER_ADMIN bootstrap account (password: Admin@12345 -- MUST be rotated after first login)
INSERT INTO users (email, phone, password, first_name, last_name, auth_provider, email_verified, phone_verified,
                    account_status, referral_code, created_at, created_by, is_deleted)
VALUES ('admin@salofresh.com', '9999999999',
        '$2a$12$xaUTuOWCxZ0siA3IN95ph.wObaWvHv8Hn2b/3WVUO1HtU4Hl3Ngcy',
        'SaloFresh', 'Admin', 'LOCAL', TRUE, TRUE, 'ACTIVE', 'SFADMIN01', CURRENT_TIMESTAMP, 'SYSTEM', FALSE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@salofresh.com' AND r.name = 'SUPER_ADMIN';

INSERT INTO wallet (user_id, balance, currency, version, created_at, created_by, is_deleted)
SELECT u.id, 0, 'INR', 0, CURRENT_TIMESTAMP, 'SYSTEM', FALSE FROM users u WHERE u.email = 'admin@salofresh.com';
