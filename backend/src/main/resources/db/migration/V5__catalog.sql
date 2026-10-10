-- V5: vendor catalog categories (reference data).

CREATE TABLE categories (
    slug VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0
);

INSERT INTO categories (slug, name, description, sort_order) VALUES
    ('photographers', 'Photographers', 'Wedding and event photographers', 1),
    ('decorators', 'Decorators', 'Mandap, stage and venue decor', 2),
    ('caterers', 'Caterers', 'Food and beverage services', 3),
    ('mehendi-artists', 'Mehendi Artists', 'Henna artists for mehendi ceremonies', 4),
    ('makeup-artists', 'Makeup Artists', 'Bridal and event makeup', 5),
    ('djs', 'DJs & Music', 'DJs, live bands and musicians', 6),
    ('venues', 'Venues', 'Banquet halls, outdoor venues', 7),
    ('planners', 'Event Planners', 'Full-service event planning', 8);
