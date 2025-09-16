-- Insert default subscription plans
INSERT INTO plans (id, name, stripe_price_id, amount, currency, interval, features, active) VALUES
(
    gen_random_uuid(),
    'Starter',
    'price_starter_monthly',
    9.99,
    'USD',
    'month',
    '{"max_users": 3, "api_calls_per_month": 10000, "support_level": "email", "custom_integrations": false, "advanced_analytics": false}'::jsonb,
    true
),
(
    gen_random_uuid(),
    'Professional',
    'price_professional_monthly',
    29.99,
    'USD',
    'month',
    '{"max_users": 10, "api_calls_per_month": 100000, "support_level": "chat", "custom_integrations": true, "advanced_analytics": true}'::jsonb,
    true
),
(
    gen_random_uuid(),
    'Enterprise',
    'price_enterprise_monthly',
    99.99,
    'USD',
    'month',
    '{"max_users": -1, "api_calls_per_month": -1, "support_level": "phone", "custom_integrations": true, "advanced_analytics": true, "priority_support": true}'::jsonb,
    true
);

-- Create trigger for automatic audit partition creation
CREATE TRIGGER audit_partition_trigger
    BEFORE INSERT ON audit_events
    FOR EACH ROW EXECUTE FUNCTION create_monthly_partition();

-- Create function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM token_metadata WHERE expires_at < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up old audit events (GDPR compliance)
CREATE OR REPLACE FUNCTION cleanup_old_audit_events(retention_months integer DEFAULT 24)
RETURNS void AS $$
DECLARE
    cutoff_date date;
BEGIN
    cutoff_date := CURRENT_DATE - (retention_months || ' months')::interval;
    DELETE FROM audit_events WHERE created_at < cutoff_date;
END;
$$ LANGUAGE plpgsql;