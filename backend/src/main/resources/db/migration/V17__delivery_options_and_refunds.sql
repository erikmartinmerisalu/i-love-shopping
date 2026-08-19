CREATE TABLE delivery_options (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    estimated_days INT NOT NULL DEFAULT 5,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE orders
    ADD COLUMN delivery_option_id BIGINT REFERENCES delivery_options(id),
    ADD COLUMN estimated_delivery_at TIMESTAMP;

CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(12, 2) NOT NULL,
    reason TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refunds_order ON refunds(order_id);
CREATE INDEX idx_orders_delivery_option ON orders(delivery_option_id);

INSERT INTO delivery_options (name, price, estimated_days, active)
VALUES
    ('Standard shipping', 4.99, 5, TRUE),
    ('Express shipping', 12.99, 2, TRUE);
