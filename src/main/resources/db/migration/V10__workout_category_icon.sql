UPDATE workout_category
SET icon_name = 'music'
WHERE icon_name = 'dance';
UPDATE workout_category
SET icon_name = 'person-standing'
WHERE icon_name = 'gymnastics';
UPDATE workout_category
SET icon_name = 'dumbbell'
WHERE icon_name = 'fitness';

UPDATE workout_category
SET icon_name = 'dumbbell'
WHERE icon_name IS NULL
   OR icon_name NOT IN ('dumbbell', 'footprints', 'volleyball', 'trophy', 'bike', 'waves',
                        'heart-pulse', 'activity', 'flame', 'mountain', 'music', 'target',
                        'timer', 'medal', 'zap', 'person-standing');

ALTER TABLE workout_category
    ALTER COLUMN icon_name SET DEFAULT 'dumbbell',
ALTER
COLUMN icon_name SET NOT NULL;

ALTER TABLE workout_category
    ADD CONSTRAINT workout_category_icon_name_check
        CHECK (icon_name IN ('dumbbell', 'footprints', 'volleyball', 'trophy', 'bike', 'waves',
                             'heart-pulse', 'activity', 'flame', 'mountain', 'music', 'target',
                             'timer', 'medal', 'zap', 'person-standing'));