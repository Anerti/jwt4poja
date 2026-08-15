CREATE TABLE IF NOT EXISTS jwt4poja_app.teacher_course (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_inheritance_id UUID        REFERENCES jwt4poja_app.teacher_inheritance(id) ON DELETE SET NULL,
    course_id              UUID        NOT NULL REFERENCES jwt4poja_app.course(id) ON DELETE CASCADE,
    assigned_at            TIMESTAMPTZ NOT NULL,
    UNIQUE (teacher_inheritance_id, course_id)
);
