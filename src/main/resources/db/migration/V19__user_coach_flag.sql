ALTER TABLE users
    ADD COLUMN IF NOT EXISTS coach boolean NOT NULL DEFAULT false;

UPDATE users
SET coach = true
WHERE id IN (SELECT coach_id FROM cooperation WHERE status IN ('PENDING', 'ACTIVE'));