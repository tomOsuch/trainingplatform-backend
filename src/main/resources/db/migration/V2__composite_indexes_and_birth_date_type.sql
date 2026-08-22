CREATE INDEX idx_training_plan_user_planned_date
    ON training_plan (user_id, planned_date);

CREATE INDEX idx_workout_log_user_performed_date
    ON workout_log (user_id, performed_date);

CREATE INDEX idx_workout_log_user_category_performed_date
    ON workout_log (user_id, category_id, performed_date);

ALTER TABLE users
    ALTER COLUMN birth_date TYPE date USING birth_date::date;