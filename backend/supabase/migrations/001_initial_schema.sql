-- Nova Vault — Supabase PostgreSQL Migrations
-- Migration 001: Initial schema
-- Run in order via: supabase db push

-- ============================================================
-- Enable required extensions
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- vault_key_envelopes
-- Stores each user's encrypted vault key envelope.
-- Server NEVER receives the plaintext DEK or master password.
-- Contains: KDF params (public), encrypted DEK ciphertext, recovery ciphertext.
-- ============================================================
CREATE TABLE IF NOT EXISTS vault_key_envelopes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    envelope_version INTEGER NOT NULL DEFAULT 1,
    envelope_json   TEXT NOT NULL,      -- JSON VaultKeyEnvelope (encrypted blobs only)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)                     -- One envelope per user
);

-- ============================================================
-- vault_items
-- Stores encrypted vault item payloads. ALL sensitive content
-- is encrypted on-device before reaching this table.
-- item_type is stored plaintext for server-side categorization
-- if needed; it reveals category but no secret content.
-- ============================================================
CREATE TABLE IF NOT EXISTS vault_items (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_type       TEXT NOT NULL,
    encrypted_payload TEXT NOT NULL,    -- Base64 AES-256-GCM ciphertext
    folder_id       UUID,
    collection_id   UUID,
    is_favorite     BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    server_version  BIGINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- folders
-- Plain folder names are stored. Consider encrypting names
-- in a future migration if folder name sensitivity is required.
-- ============================================================
CREATE TABLE IF NOT EXISTS folders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    parent_id       UUID REFERENCES folders(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- sync_metadata
-- Tracks last-sync timestamps per device for conflict resolution.
-- ============================================================
CREATE TABLE IF NOT EXISTS sync_metadata (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_id       TEXT NOT NULL,
    last_sync_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);

-- ============================================================
-- Indexes for query performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_vault_items_user_id    ON vault_items(user_id);
CREATE INDEX IF NOT EXISTS idx_vault_items_type       ON vault_items(item_type);
CREATE INDEX IF NOT EXISTS idx_vault_items_updated_at ON vault_items(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_vault_items_deleted    ON vault_items(is_deleted);
CREATE INDEX IF NOT EXISTS idx_folders_user_id        ON folders(user_id);

-- ============================================================
-- updated_at auto-update triggers
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_vault_key_envelopes_updated_at
    BEFORE UPDATE ON vault_key_envelopes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vault_items_updated_at
    BEFORE UPDATE ON vault_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_folders_updated_at
    BEFORE UPDATE ON folders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- server_version increment trigger for conflict detection
-- ============================================================
CREATE OR REPLACE FUNCTION increment_server_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.server_version = OLD.server_version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER increment_vault_item_version
    BEFORE UPDATE ON vault_items
    FOR EACH ROW EXECUTE FUNCTION increment_server_version();
