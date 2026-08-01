-- Widen columns so AES-GCM ciphertext (Base64) fits for encrypted-at-rest fields
ALTER TABLE orders
    ALTER COLUMN full_name TYPE VARCHAR(512),
    ALTER COLUMN email TYPE VARCHAR(512),
    ALTER COLUMN phone TYPE VARCHAR(512),
    ALTER COLUMN address_line1 TYPE VARCHAR(1024),
    ALTER COLUMN address_line2 TYPE VARCHAR(1024),
    ALTER COLUMN city TYPE VARCHAR(512),
    ALTER COLUMN postal_code TYPE VARCHAR(512),
    ALTER COLUMN country TYPE VARCHAR(512);

ALTER TABLE payment_transactions
    ALTER COLUMN provider_payment_id TYPE VARCHAR(512),
    ALTER COLUMN failure_message TYPE VARCHAR(1024);
