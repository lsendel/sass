-- Demo data for testing (only runs in test profile)
-- This migration creates demo organization and user for authentication testing

-- Create demo organization
INSERT INTO organizations (id, name, slug, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Organization', 'demo-org', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Create demo user with password 'DemoPassword123!'
-- Password hash is for 'DemoPassword123!' using BCrypt strength 12
INSERT INTO users (id, email, name, organization_id, password_hash, email_verified, authentication_methods, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'demo@example.com',
    'Demo User',
    '00000000-0000-0000-0000-000000000001',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY.Q/xYS8sIGbJ2', -- DemoPassword123!
    true, -- email pre-verified for demo
    'PASSWORD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email, organization_id) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    email_verified = true,
    updated_at = CURRENT_TIMESTAMP;