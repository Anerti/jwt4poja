CREATE TABLE IF NOT EXISTS jwt4poja_app."class" (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(30) NOT NULL UNIQUE,
    year_of    INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

ALTER TABLE jwt4poja_app.student_inheritance
    ADD COLUMN IF NOT EXISTS group_id UUID REFERENCES jwt4poja_app."group"(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES jwt4poja_app."class"(id) ON DELETE SET NULL;

ALTER TABLE jwt4poja_app.student_inheritance
    DROP COLUMN IF EXISTS class_name,
    DROP COLUMN IF EXISTS graduation_year;
