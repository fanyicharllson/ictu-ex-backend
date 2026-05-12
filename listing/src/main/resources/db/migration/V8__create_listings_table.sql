CREATE TABLE listings (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    condition   VARCHAR(20)  NOT NULL,
    seller_id   UUID         NOT NULL REFERENCES users(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE listing_image_urls (
    listing_id UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    image_url  VARCHAR(500) NOT NULL
);

CREATE INDEX idx_listings_status     ON listings(status);
CREATE INDEX idx_listings_seller_id  ON listings(seller_id);
CREATE INDEX idx_listings_category   ON listings(category);
