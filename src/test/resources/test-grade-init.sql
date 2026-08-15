CREATE TABLE IF NOT EXISTS jwt4poja_app.grade (
    id                     UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    student_inheritance_id UUID       NOT NULL REFERENCES jwt4poja_app.student_inheritance(id) ON DELETE CASCADE,
    exam_id                UUID       NOT NULL REFERENCES jwt4poja_app.exam(id) ON DELETE CASCADE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_inheritance_id, exam_id)
);

CREATE TABLE IF NOT EXISTS jwt4poja_app.grade_history (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    grade_id    UUID          NOT NULL REFERENCES jwt4poja_app.grade(id) ON DELETE CASCADE,
    grade       NUMERIC(4, 2) NOT NULL,
    description TEXT          NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
