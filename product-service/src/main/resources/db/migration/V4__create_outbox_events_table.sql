CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    last_error TEXT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status_created
ON outbox_events(status, created_at);

CREATE INDEX idx_outbox_events_aggregate
ON outbox_events(aggregate_type, aggregate_id);