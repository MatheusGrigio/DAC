CREATE TABLE event_outbox (
    id UUID PRIMARY KEY,
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX event_outbox_pending_idx ON event_outbox (published, created_at);
