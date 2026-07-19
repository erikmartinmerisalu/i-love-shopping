-- Create refresh tokens table for token rotation and revocation
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient token lookups and cleanup
CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_user_id_refresh ON refresh_tokens(user_id);
CREATE INDEX idx_expires_at ON refresh_tokens(expires_at);

-- Create index for finding unused/non-revoked tokens for a user
CREATE INDEX idx_user_active_tokens ON refresh_tokens(user_id, revoked, used);
