CREATE TABLE IF NOT EXISTS jwt4poja_app.grade (
    id                      UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    student_inheritance_id  UUID           NOT NULL REFERENCES jwt4poja_app.student_inheritance(id),
    exam_id                 UUID           NOT NULL REFERENCES jwt4poja_app.exam(id) ON DELETE CASCADE,
    value                   NUMERIC(10, 2) NOT NULL,
    description             TEXT           NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now()
);
