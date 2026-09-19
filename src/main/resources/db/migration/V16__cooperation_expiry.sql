ALTER TABLE cooperation
    ADD COLUMN expires_at timestamp(6);

ALTER TABLE cooperation
DROP
CONSTRAINT cooperation_status_check;

ALTER TABLE cooperation
    ADD CONSTRAINT cooperation_status_check
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'ENDED', 'EXPIRED'));

UPDATE cooperation
SET expires_at = created_at + interval '14 days'
WHERE status = 'PENDING'
  AND expires_at IS NULL;

ALTER TABLE cooperation
    ADD CONSTRAINT cooperation_pending_expiry_check
        CHECK (status <> 'PENDING' OR expires_at IS NOT NULL);
