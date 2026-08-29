-- Speed up catalog search: the storefront uses LOWER(name|brand|description) LIKE %q%,
-- not the tsvector GIN index. Trigram indexes support those LIKE patterns.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_name_lower_trgm
    ON products USING GIN (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_brand_lower_trgm
    ON products USING GIN (lower(brand) gin_trgm_ops);
