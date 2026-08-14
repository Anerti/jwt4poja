DO $$ BEGIN
    CREATE TYPE jwt4poja_app.course_type AS ENUM ('EL', 'TN', 'COMMON');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS jwt4poja_app.course (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ref        VARCHAR(10) NOT NULL UNIQUE,
    title      TEXT        NOT NULL,
    type       jwt4poja_app.course_type NOT NULL DEFAULT 'COMMON',
    credits    INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);
