UPDATE workout_category
SET color = '#' || repeat(substr(color, 2, 1), 2)
    || repeat(substr(color, 3, 1), 2)
    || repeat(substr(color, 4, 1), 2)
WHERE color ~ '^#[0-9A-Fa-f]{3}$';

UPDATE workout_category
SET color = '#6B7280'
WHERE color IS NULL
   OR color !~ '^#[0-9A-Fa-f]{6}$';

ALTER TABLE workout_category
    ALTER COLUMN color SET DEFAULT '#6B7280',
ALTER
COLUMN color SET NOT NULL;

ALTER TABLE workout_category
    ADD CONSTRAINT workout_category_color_check
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$');