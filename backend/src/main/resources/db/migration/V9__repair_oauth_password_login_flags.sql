UPDATE users
SET password_login_enabled = FALSE
WHERE provider IN ('google', 'facebook')
  AND password_login_enabled = TRUE;
