ALTER TABLE training_plan
    ADD COLUMN created_by_user_id bigint;

ALTER TABLE training_plan
    ADD CONSTRAINT fk_training_plan_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_training_plan_created_by ON training_plan (created_by_user_id);

ALTER TABLE training_plan
    ADD CONSTRAINT training_plan_author_not_owner_check
        CHECK (created_by_user_id IS NULL OR created_by_user_id <> user_id);
