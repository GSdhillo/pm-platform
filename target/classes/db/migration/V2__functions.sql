CREATE OR REPLACE FUNCTION next_issue_seq(pid uuid) RETURNS bigint AS $$
DECLARE seq bigint;
BEGIN
    UPDATE projects SET issue_seq = issue_seq + 1 WHERE id = pid RETURNING issue_seq INTO seq;
    RETURN seq;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION next_event_seq(pid uuid) RETURNS bigint AS $$
DECLARE seq bigint;
BEGIN
    UPDATE projects SET event_seq = event_seq + 1 WHERE id = pid RETURNING event_seq INTO seq;
    RETURN seq;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION acquire_tx_lock(lock_key bigint) RETURNS boolean AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(lock_key);
    RETURN true;
END;
$$ LANGUAGE plpgsql;
