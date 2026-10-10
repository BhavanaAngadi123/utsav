-- V2: vendors and government-ID verification (status-only, never ID numbers/images).

CREATE TABLE vendors (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    business_name VARCHAR(160) NOT NULL,
    category_slug VARCHAR(64) NOT NULL,
    description VARCHAR(2000),
    city VARCHAR(120) NOT NULL,
    country VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    base_price NUMERIC(12, 2),
    price_unit VARCHAR(32) NOT NULL DEFAULT 'event',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    portfolio_json TEXT,
    rating_avg NUMERIC(3, 2) NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    verified_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vendor_verified CHECK (verified_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_vendors_owner ON vendors (owner_id);
CREATE INDEX idx_vendors_category ON vendors (category_slug);
CREATE INDEX idx_vendors_city ON vendors (city);
CREATE INDEX idx_vendors_country ON vendors (country);
CREATE INDEX idx_vendors_rating ON vendors (rating_avg DESC);

-- 13-country ID verification: type + country + status ONLY.
-- Never store real government ID numbers or document images.
CREATE TABLE id_verifications (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    country_code VARCHAR(8) NOT NULL,
    id_type VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users (id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    review_note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_idv_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_idv_vendor ON id_verifications (vendor_id);
CREATE INDEX idx_idv_status ON id_verifications (status);
CREATE INDEX idx_idv_country ON id_verifications (country_code);
