-- Payment module tables
-- Customers table for payment processing
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    stripe_customer_id VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    billing_street VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(3),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Payments table
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    method VARCHAR(20) NOT NULL DEFAULT 'CARD',
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    client_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    failure_reason TEXT,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CHECK (method IN ('CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET'))
);

-- Indexes for payment tables
CREATE INDEX idx_customer_organization_user ON customers(organization_id, user_id);
CREATE INDEX idx_customer_stripe_id ON customers(stripe_customer_id);
CREATE INDEX idx_customer_email ON customers(email);

CREATE INDEX idx_payment_organization ON payments(organization_id);
CREATE INDEX idx_payment_customer ON payments(customer_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_created_at ON payments(created_at);
CREATE INDEX idx_payment_stripe_intent ON payments(stripe_payment_intent_id);
CREATE INDEX idx_payment_idempotency ON payments(idempotency_key);