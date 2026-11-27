CREATE TABLE IF NOT EXISTS booking_request_limits (
    owner_id bigint NOT NULL,
    client_address varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    request_count integer NOT NULL,
    PRIMARY KEY (owner_id, client_address)
)
^^^
CREATE INDEX IF NOT EXISTS ix_booking_request_limit_expiry ON booking_request_limits (expires_at)
^^^
