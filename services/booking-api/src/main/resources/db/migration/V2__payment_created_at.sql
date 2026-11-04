-- Lets the reconciler tell a payment still being booked from one that was captured and orphaned.
ALTER TABLE payment ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
CREATE INDEX payment_captured_idx ON payment (created_at) WHERE status = 'CAPTURED';
