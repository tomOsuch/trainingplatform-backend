ALTER TABLE training_plan
DROP CONSTRAINT fkakdfy6q2i5e7jomdkfms446ds;

ALTER TABLE training_plan
    ADD CONSTRAINT fk_training_plan_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE workout_log
DROP CONSTRAINT fk5v4tkc8cpi78w36xk6ho6cf1y;

ALTER TABLE workout_log
    ADD CONSTRAINT fk_workout_log_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE password_reset_token
DROP CONSTRAINT fk_password_reset_token_user;

ALTER TABLE password_reset_token
    ADD CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE invitation
    ALTER COLUMN invited_by DROP NOT NULL;

ALTER TABLE invitation
DROP CONSTRAINT fk_invitation_invited_by;

ALTER TABLE invitation
    ADD CONSTRAINT fk_invitation_invited_by
        FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE SET NULL;