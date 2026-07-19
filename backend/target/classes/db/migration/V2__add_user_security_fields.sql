-- Add user security and authentication fields
ALTER TABLE users ADD COLUMN username VARCHAR(100);
ALTER TABLE users ADD COLUMN provider VARCHAR(50);
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Populate username from email (first part before @) with ID to ensure uniqueness
UPDATE users SET username = CONCAT(SPLIT_PART(email, '@', 1), '_', id);

-- Add NOT NULL constraint after populating
ALTER TABLE users ALTER COLUMN username SET NOT NULL;

-- Create index (non-unique) on username for fast lookups
CREATE INDEX idx_username ON users(username);

-- Create index on provider for OAuth lookups
CREATE INDEX idx_provider ON users(provider);
