DO $$ BEGIN
    CREATE TYPE jwt4poja_app.group_type AS ENUM ('EL', 'TN', 'COMMON');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS jwt4poja_app."group" (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ref        VARCHAR(10) NOT NULL UNIQUE,
    type       jwt4poja_app.group_type NOT NULL DEFAULT 'COMMON',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
