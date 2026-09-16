INSERT INTO workout_category (name, color, icon_name)
VALUES ('Taniec', '#9B59B6', 'music'),
       ('Gimnastyka', '#E74C3C', 'person-standing'),
       ('Ogólnorozwojowy', '#27AE60', 'dumbbell') ON CONFLICT (name) DO NOTHING;