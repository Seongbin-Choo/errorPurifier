ALTER TABLE parsing_audit_log MODIFY created_at DATETIME(6) NOT NULL;
CREATE INDEX idx_audit_created_at_id ON parsing_audit_log (created_at, id);
