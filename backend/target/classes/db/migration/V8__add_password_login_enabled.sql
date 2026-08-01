ALTER TABLE users ADD COLUMN password_login_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users
SET password_login_enabled = FALSE
WHERE provider IN ('google', 'facebook');
