CREATE TABLE IF NOT EXISTS client_device (
    id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    quota_date DATE NOT NULL,
    daily_request_count INT NOT NULL,
    plugin_version VARCHAR(20) NOT NULL,
    blocked_reason VARCHAR(500),
    blocked_at DATETIME(6),
    last_access_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_last_access_at ON client_device (last_access_at);

CREATE TABLE IF NOT EXISTS error_cache (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cache_key VARCHAR(64) NOT NULL,
    environment_tags JSON NOT NULL,
    exception_type VARCHAR(255) NOT NULL,
    solution_text TEXT NOT NULL,
    saved_tokens INT NOT NULL,
    hit_count INT NOT NULL,
    success_count INT NOT NULL,
    report_count INT NOT NULL,
    is_blinded BOOLEAN NOT NULL,
    version BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_error_cache_key UNIQUE (cache_key)
);

CREATE TABLE IF NOT EXISTS log_parsing_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_type VARCHAR(20) NOT NULL,
    target_framework VARCHAR(50) NOT NULL,
    regex_pattern VARCHAR(500) NOT NULL,
    priority INT NOT NULL,
    description VARCHAR(255),
    min_plugin_version VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS diagnostic_playbook (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    match_pattern VARCHAR(1000) NOT NULL,
    guidance TEXT NOT NULL,
    priority INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_diagnostic_playbook_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS request_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BINARY(16) NOT NULL,
    cache_id BIGINT,
    request_type VARCHAR(20) NOT NULL,
    processing_time_ms INT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_request_history_device FOREIGN KEY (device_id) REFERENCES client_device (id),
    CONSTRAINT fk_request_history_cache FOREIGN KEY (cache_id) REFERENCES error_cache (id)
);

CREATE TABLE IF NOT EXISTS parsing_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BINARY(16) NOT NULL,
    linked_cache_id BIGINT,
    issue_type VARCHAR(30) NOT NULL,
    raw_log_content TEXT NOT NULL,
    parsed_log_content TEXT,
    user_comment VARCHAR(500),
    is_masked BOOLEAN NOT NULL,
    is_reviewed BOOLEAN NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_parsing_audit_device FOREIGN KEY (device_id) REFERENCES client_device (id),
    CONSTRAINT fk_parsing_audit_cache FOREIGN KEY (linked_cache_id) REFERENCES error_cache (id)
);

CREATE TABLE IF NOT EXISTS llm_usage_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BINARY(16) NOT NULL,
    cache_id BIGINT,
    cache_hit BOOLEAN NOT NULL,
    provider VARCHAR(20) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_hash VARCHAR(64) NOT NULL,
    original_characters INT NOT NULL,
    prepared_characters INT NOT NULL,
    input_tokens INT NOT NULL,
    output_tokens INT NOT NULL,
    total_tokens INT NOT NULL,
    latency_ms BIGINT NOT NULL,
    referenced_lines TEXT,
    rating INT NOT NULL,
    resolved BOOLEAN NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_llm_usage_device FOREIGN KEY (device_id) REFERENCES client_device (id),
    CONSTRAINT fk_llm_usage_cache FOREIGN KEY (cache_id) REFERENCES error_cache (id)
);

CREATE INDEX IF NOT EXISTS idx_usage_created_at ON llm_usage_log (created_at);
CREATE INDEX IF NOT EXISTS idx_usage_provider_model ON llm_usage_log (provider, model);

CREATE TABLE IF NOT EXISTS refinement_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BINARY(16) NOT NULL,
    cache_id BIGINT,
    feedback_type VARCHAR(30) NOT NULL,
    original_characters INT NOT NULL,
    prepared_characters INT NOT NULL,
    applied_rule_counts JSON NOT NULL,
    protected_line_count INT NOT NULL,
    log_truncated BOOLEAN NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_refinement_feedback_device FOREIGN KEY (device_id) REFERENCES client_device (id),
    CONSTRAINT fk_refinement_feedback_cache FOREIGN KEY (cache_id) REFERENCES error_cache (id)
);

CREATE INDEX IF NOT EXISTS idx_refinement_feedback_created ON refinement_feedback (created_at);
CREATE INDEX IF NOT EXISTS idx_refinement_feedback_type ON refinement_feedback (feedback_type);
