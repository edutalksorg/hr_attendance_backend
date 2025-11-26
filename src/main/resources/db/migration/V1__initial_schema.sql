-- V1__initial_schema.sql
-- Initial schema for MegaMart: users, roles, login history, navigation, docs, approvals, salary, notifications, refresh tokens

-- Extensions (for UUIDs)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- user_role enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE');
    END IF;
END$$;

-- user_status enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
        CREATE TYPE user_status AS ENUM ('PENDING','ACTIVE','INACTIVE','BLOCKED');
    END IF;
END$$;

-- users table
CREATE TABLE IF NOT EXISTS users (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name  text,
  email      text NOT NULL UNIQUE,
  phone      text,
  password_hash text NOT NULL,
  role       user_role NOT NULL DEFAULT 'EMPLOYEE',
  status     user_status NOT NULL DEFAULT 'PENDING',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- login history (stores IP, login/logout timestamps) - retained 60 days via scheduled job
CREATE TABLE IF NOT EXISTS login_history (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  login_time timestamptz NOT NULL DEFAULT now(),
  logout_time timestamptz,
  ip_address text,
  user_agent text,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_login_history_user ON login_history(user_id);
CREATE INDEX IF NOT EXISTS idx_login_history_created ON login_history(created_at);

-- navigation history for marketing executive (no IP) - retained 60 days via scheduled job
CREATE TABLE IF NOT EXISTS navigation_history (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  path       text NOT NULL,        -- navigation path / screen / route
  metadata   jsonb,               -- optional extra data (duration, coords, etc.)
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_nav_user ON navigation_history(user_id);
CREATE INDEX IF NOT EXISTS idx_nav_created ON navigation_history(created_at);

-- documents table (generated when HR/Admin approves)
CREATE TABLE IF NOT EXISTS documents (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       text NOT NULL,          -- e.g. 'offer_letter','joining_docs','salary_slip','contract'
  file_path  text NOT NULL,          -- storage path / object key
  generated_by uuid,                 -- who generated (HR/Admin)
  generated_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_documents_user ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_generated_at ON documents(generated_at);

-- salary slips table (separate for structured data)
CREATE TABLE IF NOT EXISTS salary_slips (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  period     text NOT NULL,         -- e.g. '2025-11'
  gross      numeric(14,2) NOT NULL DEFAULT 0,
  net        numeric(14,2) NOT NULL DEFAULT 0,
  deductions numeric(14,2) NOT NULL DEFAULT 0,
  file_path  text,                  -- PDF path
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_salary_user_period ON salary_slips(user_id, period);

-- approvals table (HR/Admin approvals for employee registration / docs)
CREATE TABLE IF NOT EXISTS approvals (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  target_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  requested_by  uuid,                -- who requested (employee or HR)
  approved_by   uuid,                -- admin or hr id who approved
  role_after    user_role,           -- role to set when approved (if registration)
  approval_type text NOT NULL,       -- e.g. 'REGISTRATION','DOCUMENT','SALARY_SLIP'
  status        text NOT NULL,       -- 'PENDING','APPROVED','REJECTED'
  reason        text,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_approvals_target ON approvals(target_user_id);
CREATE INDEX IF NOT EXISTS idx_approvals_status ON approvals(status);

-- notifications table
CREATE TABLE IF NOT EXISTS notifications (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title      text,
  message    text,
  is_read    boolean NOT NULL DEFAULT false,
  source     text,                 -- 'SYSTEM','HR','ADMIN'
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

-- refresh tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token      text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  revoked    boolean NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_tokens(user_id);

-- audit table for marketing exec history visible to admin/hr (kept 60 days)
CREATE TABLE IF NOT EXISTS marketing_history (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  marketing_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  action     text NOT NULL,
  metadata   jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_marketing_created ON marketing_history(created_at);

-- helper function: keep updated_at in users automatically
CREATE OR REPLACE FUNCTION trg_set_timestamp()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_timestamp ON users;
CREATE TRIGGER set_timestamp BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();

-- Optional: create roles view for convenience
CREATE OR REPLACE VIEW vw_users_basic AS
SELECT id, full_name, email, role, status, created_at FROM users;
