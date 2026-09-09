ALTER TABLE training_plan
    ADD COLUMN reminder_sent_at timestamp(6);

CREATE INDEX idx_training_plan_reminder_pending
    ON training_plan (planned_date) WHERE reminder_sent_at IS NULL AND status = 'PLANNED';