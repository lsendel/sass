-- Subscription module tables
-- Plans table
CREATE TABLE plans (
    plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    features TEXT, -- JSON string of features
    is_active BOOLEAN NOT NULL DEFAULT true,
    stripe_price_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CHECK (billing_interval IN ('MONTHLY', 'QUARTERLY', 'YEARLY'))
);

-- Subscriptions table
CREATE TABLE subscriptions (
    subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    next_billing_date DATE,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    canceled_at TIMESTAMP,
    cancellation_reason TEXT,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (plan_id) REFERENCES plans(plan_id),
    CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELED', 'PAST_DUE', 'INCOMPLETE')),
    CHECK (billing_interval IN ('MONTHLY', 'QUARTERLY', 'YEARLY'))
);

-- Invoices table
CREATE TABLE invoices (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    stripe_invoice_id VARCHAR(255) UNIQUE,
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    due_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(subscription_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE'))
);

-- Indexes for subscription tables
CREATE INDEX idx_plan_active ON plans(is_active);
CREATE INDEX idx_plan_stripe_price ON plans(stripe_price_id);

CREATE INDEX idx_subscription_organization ON subscriptions(organization_id);
CREATE INDEX idx_subscription_customer ON subscriptions(customer_id);
CREATE INDEX idx_subscription_plan ON subscriptions(plan_id);
CREATE INDEX idx_subscription_status ON subscriptions(status);
CREATE INDEX idx_subscription_billing_date ON subscriptions(next_billing_date);
CREATE INDEX idx_subscription_stripe_id ON subscriptions(stripe_subscription_id);

CREATE INDEX idx_invoice_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoice_organization ON invoices(organization_id);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_due_date ON invoices(due_date);
CREATE INDEX idx_invoice_stripe_id ON invoices(stripe_invoice_id);
CREATE INDEX idx_invoice_number ON invoices(invoice_number);