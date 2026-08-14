CREATE TABLE IF NOT EXISTS jwt4poja_app.student_group (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_inheritance_id  UUID        NOT NULL REFERENCES jwt4poja_app.student_inheritance(id) ON DELETE CASCADE,
    group_id                UUID        NOT NULL REFERENCES jwt4poja_app."group"(id) ON DELETE CASCADE,
    joigned_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at                 TIMESTAMPTZ,
    UNIQUE (student_inheritance_id, group_id)
);
