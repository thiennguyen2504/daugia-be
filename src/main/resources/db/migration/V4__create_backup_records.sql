CREATE TABLE IF NOT EXISTS backup_records (
  id VARCHAR(36) PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  file_path VARCHAR(512),
  file_name VARCHAR(255),
  file_size_bytes BIGINT,
  checksum_sha256 VARCHAR(64),
  duration_ms BIGINT,
  triggered_by VARCHAR(255),
  error_message TEXT,
  created_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_backup_status ON backup_records(status);
CREATE INDEX IF NOT EXISTS idx_backup_created ON backup_records(created_at);
CREATE INDEX IF NOT EXISTS idx_backup_type ON backup_records(type);
