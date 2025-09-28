-- V018: Create audit log export tracking table
-- Description: Table to track audit log export requests and their status
-- Date: 2025-09-27

-- Create the audit_log_exports table
CREATE TABLE audit_log_exports (
    export_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    filter_criteria JSONB NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('CSV', 'JSON', 'PDF')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    download_token VARCHAR(255) UNIQUE,
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    entry_count INTEGER,
    expires_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_completed_at_after_requested CHECK (completed_at IS NULL OR completed_at >= requested_at),
    CONSTRAINT chk_expires_at_after_completed CHECK (expires_at IS NULL OR completed_at IS NULL OR expires_at > completed_at),
    CONSTRAINT chk_file_info_with_completion CHECK (
        (status = 'COMPLETED' AND download_token IS NOT NULL AND file_path IS NOT NULL AND entry_count IS NOT NULL) OR
        (status != 'COMPLETED')
    ),
    CONSTRAINT chk_error_message_with_failure CHECK (
        (status = 'FAILED' AND error_message IS NOT NULL) OR
        (status != 'FAILED')
    ),
    CONSTRAINT chk_entry_count_positive CHECK (entry_count IS NULL OR entry_count >= 0),
    CONSTRAINT chk_file_size_positive CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0)
);

-- Indexes for performance
CREATE INDEX idx_audit_exports_user_requested ON audit_log_exports(user_id, requested_at DESC);
CREATE INDEX idx_audit_exports_org_requested ON audit_log_exports(organization_id, requested_at DESC);
CREATE INDEX idx_audit_exports_status ON audit_log_exports(status, requested_at DESC);
CREATE INDEX idx_audit_exports_token ON audit_log_exports(download_token) WHERE download_token IS NOT NULL;
CREATE INDEX idx_audit_exports_cleanup ON audit_log_exports(expires_at) WHERE status = 'COMPLETED' AND expires_at IS NOT NULL;

-- Foreign key relationships (referencing existing tables)
ALTER TABLE audit_log_exports
ADD CONSTRAINT fk_audit_exports_user
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE audit_log_exports
ADD CONSTRAINT fk_audit_exports_organization
FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_audit_export_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_exports_updated_at
    BEFORE UPDATE ON audit_log_exports
    FOR EACH ROW
    EXECUTE FUNCTION update_audit_export_updated_at();

-- Add comments for documentation
COMMENT ON TABLE audit_log_exports IS 'Tracks audit log export requests and their processing status';
COMMENT ON COLUMN audit_log_exports.export_id IS 'Unique identifier for the export request';
COMMENT ON COLUMN audit_log_exports.user_id IS 'User who requested the export';
COMMENT ON COLUMN audit_log_exports.organization_id IS 'Organization context for the export';
COMMENT ON COLUMN audit_log_exports.filter_criteria IS 'JSON containing the filter criteria applied to the export';
COMMENT ON COLUMN audit_log_exports.format IS 'Export format: CSV, JSON, or PDF';
COMMENT ON COLUMN audit_log_exports.status IS 'Current status of the export processing';
COMMENT ON COLUMN audit_log_exports.download_token IS 'Secure token for downloading the completed export';
COMMENT ON COLUMN audit_log_exports.file_path IS 'Server-side path to the generated export file';
COMMENT ON COLUMN audit_log_exports.expires_at IS 'When the download link expires (24 hours after completion)';
COMMENT ON COLUMN audit_log_exports.entry_count IS 'Number of audit entries included in the export';
COMMENT ON COLUMN audit_log_exports.file_size_bytes IS 'Size of the generated export file in bytes';

-- Sample data cleanup policy (to be implemented in application)
COMMENT ON INDEX idx_audit_exports_cleanup IS 'Index for cleaning up expired export files';