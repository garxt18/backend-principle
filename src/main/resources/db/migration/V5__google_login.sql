-- =====================================================================
-- V5: "Continue with Google". A user can be linked to one Google account
-- (the OpenID Connect "sub" claim - stable even if the Gmail address
-- changes). Accounts created through Google have no usable password until
-- the owner sets one in Settings.
-- =====================================================================
ALTER TABLE users
    ADD COLUMN google_subject VARCHAR(255) UNIQUE,
    ADD COLUMN has_password   BOOLEAN NOT NULL DEFAULT TRUE;
