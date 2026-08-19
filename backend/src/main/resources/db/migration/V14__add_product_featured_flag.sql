ALTER TABLE products ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

-- Highlight top-rated products on the home page
UPDATE products
SET featured = TRUE
WHERE id IN (
    SELECT id FROM products ORDER BY rating DESC, name ASC LIMIT 4
);

CREATE INDEX idx_products_featured ON products (featured) WHERE featured = TRUE;
