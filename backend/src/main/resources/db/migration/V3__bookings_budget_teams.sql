-- V3: bookings, Budget Freeze, team builder.

CREATE TABLE vendor_slots (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    slot_label VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slot_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED')),
    CONSTRAINT uq_vendor_slot UNIQUE (vendor_id, slot_date, slot_label)
);

CREATE INDEX idx_slots_vendor_date ON vendor_slots (vendor_id, slot_date);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    slot_id UUID REFERENCES vendor_slots (id) ON DELETE SET NULL,
    occasion_slug VARCHAR(64),
    event_date DATE,
    slot_label VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    total_amount NUMERIC(12, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_booking_payment CHECK (payment_status IN ('UNPAID', 'DEPOSIT_PAID', 'PAID', 'REFUNDED', 'FAILED'))
);

CREATE INDEX idx_bookings_customer ON bookings (customer_id);
CREATE INDEX idx_bookings_vendor ON bookings (vendor_id);
CREATE INDEX idx_bookings_date ON bookings (event_date);

-- Budget Freeze: lock guest count + max budget; discovery filters to fit.
CREATE TABLE budget_freezes (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    guest_count INT NOT NULL,
    max_budget NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_freeze_guests CHECK (guest_count > 0),
    CONSTRAINT chk_freeze_budget CHECK (max_budget > 0)
);

CREATE INDEX idx_freeze_customer ON budget_freezes (customer_id);

-- Team builder: customer assembles a vendor team for an event.
CREATE TABLE event_teams (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    occasion_slug VARCHAR(64),
    event_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_teams_customer ON event_teams (customer_id);

CREATE TABLE event_team_members (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES event_teams (id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    role_label VARCHAR(64) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_team_vendor UNIQUE (team_id, vendor_id)
);

CREATE INDEX idx_team_members_team ON event_team_members (team_id);

-- Reviews
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating INT NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uq_review_booking UNIQUE (booking_id)
);

CREATE INDEX idx_reviews_vendor ON reviews (vendor_id);
