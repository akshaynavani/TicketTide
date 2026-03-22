CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    order_id        UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    amount_cents    BIGINT       NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    processor_ref   VARCHAR(100),
    failure_reason  VARCHAR(200),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_payments_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_user ON payments (user_id, created_at DESC);

-- An order can only ever be captured once, whatever idempotency key the client sends.
CREATE UNIQUE INDEX uq_payments_order_captured ON payments (order_id)
    WHERE status IN ('PENDING', 'SUCCEEDED');
