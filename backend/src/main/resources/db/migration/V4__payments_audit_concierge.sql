-- V4: payments, audit log, concierge chat history.

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL DEFAULT 'stripe',
    provider_payment_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    kind VARCHAR(20) NOT NULL DEFAULT 'DEPOSIT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_kind CHECK (kind IN ('DEPOSIT', 'FULL', 'REFUND')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED', 'CANCELLED'))
);

CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_customer ON payments (customer_id);
CREATE INDEX idx_payments_vendor ON payments (vendor_id);
CREATE INDEX idx_payments_provider_id ON payments (provider_payment_id);

-- Audit log for sensitive actions (logins, role changes, verifications, payments, refunds).
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users (id) ON DELETE SET NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    detail VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_created ON audit_logs (created_at);

-- Concierge chat history
CREATE TABLE concierge_sessions (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE concierge_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES concierge_sessions (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_msg_role CHECK (role IN ('user', 'assistant', 'system'))
);

CREATE INDEX idx_concierge_session ON concierge_messages (session_id);
