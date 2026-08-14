DO $$ BEGIN
    CREATE TYPE jwt4poja_app.user_role AS ENUM ('ADMIN', 'TEACHER', 'STUDENT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE jwt4poja_app.teacher_status AS ENUM ('ACTIVE', 'INACTIVE', 'OTHER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE jwt4poja_app.student_level AS ENUM ('L1', 'L2', 'L3');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE jwt4poja_app.learning_path AS ENUM ('EL', 'TN', 'COMMON');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE jwt4poja_app.student_status AS ENUM ('GRADUATED', 'ACTIVE', 'INACTIVE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS jwt4poja_app.teacher_inheritance (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ref            VARCHAR(10) NOT NULL UNIQUE,
    joigned_at     TIMESTAMPTZ,
    teacher_status jwt4poja_app.teacher_status NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS jwt4poja_app.student_inheritance (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ref             VARCHAR(10) NOT NULL UNIQUE,
    joigned_at      TIMESTAMPTZ,
    level           jwt4poja_app.student_level NOT NULL,
    learning_path   jwt4poja_app.learning_path NOT NULL DEFAULT 'COMMON',
    student_status  jwt4poja_app.student_status NOT NULL DEFAULT 'ACTIVE',
    graduation_year INT,
    class_name      VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS jwt4poja_app."user" (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username               VARCHAR(50) NOT NULL UNIQUE,
    password               VARCHAR(255) NOT NULL,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    email                  VARCHAR(100) NOT NULL UNIQUE,
    teacher_inheritance_id UUID        REFERENCES jwt4poja_app.teacher_inheritance(id),
    student_inheritance_id UUID        REFERENCES jwt4poja_app.student_inheritance(id),
    role                   jwt4poja_app.user_role NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ
);
