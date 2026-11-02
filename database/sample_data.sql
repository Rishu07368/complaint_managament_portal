-- Optional category seed. On application startup, the same defaults are added if no categories exist.
USE complaint_portal;
INSERT IGNORE INTO complaint_categories (name, description) VALUES
('Facilities', 'Building, equipment, and maintenance concerns'),
('IT Support', 'Accounts, devices, software, and network issues'),
('Billing', 'Invoices, payments, and account charges'),
('Customer Service', 'Service quality and communication'),
('Other', 'Issues that do not fit another category');

-- Demonstration administrator; the password value is a BCrypt hash, never plaintext.
INSERT IGNORE INTO users (full_name, email, password_hash, role) VALUES
('Portal Administrator', 'admin@complaints.local',
 '$2a$12$uy2zjgnrwLWGorc3yDAEHOS48oPYEkZmFFSKO6pgKcbXkOj94J1x.', 'ADMIN');

-- Default demonstration login: admin@complaints.local / Admin@123
INSERT IGNORE INTO complaints
    (complaint_code, user_id, category_id, title, description, priority, status, assigned_admin_id, admin_remarks, resolved_at)
VALUES
('CMP-DEMO-0001',
 (SELECT id FROM users WHERE email = 'admin@complaints.local'),
 (SELECT id FROM complaint_categories WHERE name = 'Facilities'),
 'Meeting room display is offline', 'The display in meeting room 2 does not power on.', 'MEDIUM', 'PENDING',
 NULL, NULL, NULL),
('CMP-DEMO-0002',
 (SELECT id FROM users WHERE email = 'admin@complaints.local'),
 (SELECT id FROM complaint_categories WHERE name = 'IT Support'),
 'VPN connection drops frequently', 'The VPN disconnects several times during the workday.', 'HIGH', 'IN_PROGRESS',
 (SELECT id FROM users WHERE email = 'admin@complaints.local'), 'Investigating network logs.', NULL),
('CMP-DEMO-0003',
 (SELECT id FROM users WHERE email = 'admin@complaints.local'),
 (SELECT id FROM complaint_categories WHERE name = 'Customer Service'),
 'Request for a billing clarification', 'A previous invoice required additional explanation.', 'LOW', 'RESOLVED',
 (SELECT id FROM users WHERE email = 'admin@complaints.local'), 'Clarification provided.', CURRENT_TIMESTAMP);
