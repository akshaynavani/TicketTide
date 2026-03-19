CREATE TABLE events (
    id           UUID PRIMARY KEY,
    organizer_id UUID         NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    venue        VARCHAR(200) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    category     VARCHAR(50),
    starts_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    image_key    VARCHAR(300),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_events_time CHECK (ends_at > starts_at)
);

CREATE INDEX idx_events_status_starts ON events (status, starts_at);
CREATE INDEX idx_events_organizer ON events (organizer_id);

CREATE TABLE ticket_types (
    id          UUID PRIMARY KEY,
    event_id    UUID         NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    price_cents BIGINT       NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    capacity    INTEGER      NOT NULL,
    available   INTEGER      NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_ticket_types_price CHECK (price_cents >= 0),
    CONSTRAINT ck_ticket_types_available CHECK (available >= 0 AND available <= capacity)
);

CREATE INDEX idx_ticket_types_event ON ticket_types (event_id);
