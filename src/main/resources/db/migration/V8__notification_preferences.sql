ALTER TABLE users
    ADD COLUMN reminders_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN reminder_hours_before integer NOT NULL DEFAULT 24;

ALTER TABLE users
    ADD CONSTRAINT users_reminder_hours_before_check
        CHECK (reminder_hours_before BETWEEN 1 AND 168);