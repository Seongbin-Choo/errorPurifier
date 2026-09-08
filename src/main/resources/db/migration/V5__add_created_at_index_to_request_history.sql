ALTER TABLE request_history MODIFY created_at DATETIME(6) NOT NULL;
CREATE INDEX idx_history_created_at_id ON request_history (created_at, id);
