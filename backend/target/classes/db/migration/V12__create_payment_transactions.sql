-- Payment transaction records (provider refs only — never raw card data)
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_payment_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_order ON payment_transactions(order_id);
CREATE INDEX idx_payment_transactions_provider_id ON payment_transactions(provider_payment_id);
CREATE UNIQUE INDEX uq_payment_provider_payment_id
    ON payment_transactions(provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;
