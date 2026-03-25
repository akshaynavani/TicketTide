CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL,
    email        VARCHAR(255),
    type         VARCHAR(50)  NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    email_status VARCHAR(20)  NOT NULL,
    read_at      TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
