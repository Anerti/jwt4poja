CREATE TABLE IF NOT EXISTS jwt4poja_app.exam (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id     UUID           NOT NULL REFERENCES jwt4poja_app.course(id) ON DELETE CASCADE,
    coefficient   NUMERIC(10, 2) NOT NULL,
    academic_year VARCHAR(10)    NOT NULL,
    date          TIMESTAMPTZ    NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);
