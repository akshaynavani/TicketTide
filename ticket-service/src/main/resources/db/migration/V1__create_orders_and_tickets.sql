CREATE TABLE orders (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL,
    user_email        VARCHAR(255) NOT NULL,
    event_id          UUID         NOT NULL,
    event_title       VARCHAR(200) NOT NULL,
    ticket_type_id    UUID         NOT NULL,
    ticket_type_name  VARCHAR(100) NOT NULL,
    quantity          INTEGER      NOT NULL,
    unit_price_cents  BIGINT       NOT NULL,
    total_cents       BIGINT       NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    payment_id        UUID,
    expires_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_orders_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_orders_user ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_pending_expiry ON orders (status, expires_at);

CREATE TABLE tickets (
    id             UUID PRIMARY KEY,
    order_id       UUID        NOT NULL REFERENCES orders (id),
    user_id        UUID        NOT NULL,
    event_id       UUID        NOT NULL,
    ticket_type_id UUID        NOT NULL,
    code           VARCHAR(16) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    issued_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    checked_in_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tickets_code UNIQUE (code)
);

CREATE INDEX idx_tickets_user ON tickets (user_id);
CREATE INDEX idx_tickets_order ON tickets (order_id);
