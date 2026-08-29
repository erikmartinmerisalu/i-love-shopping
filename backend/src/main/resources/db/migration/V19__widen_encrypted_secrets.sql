-- Encrypt-at-rest column widths for 2FA secrets and order line snapshots
ALTER TABLE users
    ALTER COLUMN two_factor_secret TYPE VARCHAR(512);

ALTER TABLE order_items
    ALTER COLUMN product_name TYPE VARCHAR(1024);
