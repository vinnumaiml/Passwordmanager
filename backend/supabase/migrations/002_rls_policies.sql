-- Nova Vault — Row Level Security Policies
-- Migration 002: RLS policies
-- CRITICAL: These policies ensure users can ONLY access their own data.
-- The server acts as a zero-knowledge store — it cannot decrypt vault contents.

-- ============================================================
-- Enable Row Level Security on all tables
-- ============================================================
ALTER TABLE vault_key_envelopes ENABLE ROW LEVEL SECURITY;
ALTER TABLE vault_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_metadata ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- vault_key_envelopes policies
-- ============================================================

-- Users can only read their own key envelope
CREATE POLICY "Users can read own key envelope"
    ON vault_key_envelopes FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Users can only insert their own key envelope
CREATE POLICY "Users can insert own key envelope"
    ON vault_key_envelopes FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

-- Users can only update their own key envelope
CREATE POLICY "Users can update own key envelope"
    ON vault_key_envelopes FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Users can only delete their own key envelope
CREATE POLICY "Users can delete own key envelope"
    ON vault_key_envelopes FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- ============================================================
-- vault_items policies
-- ============================================================

CREATE POLICY "Users can read own vault items"
    ON vault_items FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own vault items"
    ON vault_items FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own vault items"
    ON vault_items FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own vault items"
    ON vault_items FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- ============================================================
-- folders policies
-- ============================================================

CREATE POLICY "Users can read own folders"
    ON folders FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own folders"
    ON folders FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own folders"
    ON folders FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own folders"
    ON folders FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- ============================================================
-- sync_metadata policies
-- ============================================================

CREATE POLICY "Users can manage own sync metadata"
    ON sync_metadata FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================================
-- Account deletion Edge Function hook
-- This function is called by a Supabase Edge Function on account deletion.
-- It deletes all user data before removing the auth.users record.
-- The Edge Function must be secured with service-role key (server-side only).
-- ============================================================
CREATE OR REPLACE FUNCTION delete_user_data(target_user_id UUID)
RETURNS void
SECURITY DEFINER
SET search_path = public
LANGUAGE plpgsql AS $$
BEGIN
    -- Verify caller is service role (not exploitable by client anon key)
    IF auth.role() != 'service_role' THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;
    DELETE FROM vault_items WHERE user_id = target_user_id;
    DELETE FROM folders WHERE user_id = target_user_id;
    DELETE FROM vault_key_envelopes WHERE user_id = target_user_id;
    DELETE FROM sync_metadata WHERE user_id = target_user_id;
END;
$$;
