ALTER TABLE workout_category
DROP
CONSTRAINT workout_category_icon_name_check;

UPDATE workout_category
SET icon_name = 'waves-horizontal'
WHERE icon_name = 'waves';

ALTER TABLE workout_category
    ADD CONSTRAINT workout_category_icon_name_check
        CHECK (icon_name IN ('dumbbell', 'footprints', 'volleyball', 'trophy', 'bike', 'waves-horizontal',
                             'heart-pulse', 'activity', 'flame', 'mountain', 'music', 'target',
                             'timer', 'medal', 'zap', 'person-standing'));