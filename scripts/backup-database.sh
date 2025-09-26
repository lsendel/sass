#!/bin/bash
set -euo pipefail

# Database Backup Script for Payment Platform
# This script creates automated backups of PostgreSQL database
# Usage: ./scripts/backup-database.sh [environment] [backup-type]

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENVIRONMENT="${1:-production}"
BACKUP_TYPE="${2:-full}"
TIMESTAMP=$(date -u '+%Y%m%d_%H%M%S')

# Database configuration from environment variables or defaults
DB_HOST="${PGHOST:-localhost}"
DB_PORT="${PGPORT:-5432}"
DB_NAME="${PGDATABASE:-platform}"
DB_USER="${PGUSER:-platform}"
DB_PASSWORD="${PGPASSWORD}"

# Backup configuration
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
COMPRESSION_LEVEL="${COMPRESSION_LEVEL:-6}"

# Logging configuration
LOG_FILE="${BACKUP_DIR}/backup.log"
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    local message="$1"
    echo -e "${BLUE}[INFO $(date -u '+%Y-%m-%d %H:%M:%S UTC')]${NC} $message" | tee -a "$LOG_FILE"
}

log_success() {
    local message="$1"
    echo -e "${GREEN}[SUCCESS $(date -u '+%Y-%m-%d %H:%M:%S UTC')]${NC} $message" | tee -a "$LOG_FILE"
}

log_warning() {
    local message="$1"
    echo -e "${YELLOW}[WARNING $(date -u '+%Y-%m-%d %H:%M:%S UTC')]${NC} $message" | tee -a "$LOG_FILE"
}

log_error() {
    local message="$1"
    echo -e "${RED}[ERROR $(date -u '+%Y-%m-%d %H:%M:%S UTC')]${NC} $message" | tee -a "$LOG_FILE"
}

# Error handling
handle_error() {
    local exit_code=$?
    log_error "Backup failed with exit code $exit_code at line $1"
    send_notification "FAILED" "Database backup failed with exit code $exit_code"
    exit $exit_code
}

trap 'handle_error $LINENO' ERR

# Function to send notifications
send_notification() {
    local status="$1"
    local message="$2"

    # Send Slack notification if webhook URL is configured
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        local color="good"
        local emoji="✅"

        if [[ "$status" == "FAILED" ]]; then
            color="danger"
            emoji="❌"
        elif [[ "$status" == "WARNING" ]]; then
            color="warning"
            emoji="⚠️"
        fi

        local payload=$(cat <<EOF
{
    "attachments": [
        {
            "color": "$color",
            "fields": [
                {
                    "title": "$emoji Database Backup - $ENVIRONMENT",
                    "value": "$message",
                    "short": false
                },
                {
                    "title": "Environment",
                    "value": "$ENVIRONMENT",
                    "short": true
                },
                {
                    "title": "Timestamp",
                    "value": "$(date -u '+%Y-%m-%d %H:%M:%S UTC')",
                    "short": true
                }
            ]
        }
    ]
}
EOF
        )

        curl -s -X POST -H 'Content-type: application/json' \
            --data "$payload" \
            "$SLACK_WEBHOOK_URL" > /dev/null || log_warning "Failed to send Slack notification"
    fi
}

# Function to validate prerequisites
validate_prerequisites() {
    log_info "Validating prerequisites..."

    # Check if required tools are installed
    local required_tools=("pg_dump" "gzip" "aws" "gpg")
    for tool in "${required_tools[@]}"; do
        if command -v "$tool" >/dev/null 2>&1; then
            log_info "✓ $tool is available"
        else
            case "$tool" in
                "aws"|"gpg")
                    log_warning "⚠ $tool is not available (optional)"
                    ;;
                *)
                    log_error "✗ $tool is required but not available"
                    exit 1
                    ;;
            esac
        fi
    done

    # Create backup directory if it doesn't exist
    mkdir -p "$BACKUP_DIR"
    if [[ ! -w "$BACKUP_DIR" ]]; then
        log_error "Backup directory $BACKUP_DIR is not writable"
        exit 1
    fi

    # Validate database connection
    if ! PGPASSWORD="$DB_PASSWORD" pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
        log_error "Cannot connect to database $DB_NAME on $DB_HOST:$DB_PORT"
        exit 1
    fi

    log_success "Prerequisites validation completed"
}

# Function to perform database backup
perform_backup() {
    local backup_name="${ENVIRONMENT}_${BACKUP_TYPE}_${TIMESTAMP}"
    local backup_file="${BACKUP_DIR}/${backup_name}.sql"
    local compressed_backup="${backup_file}.gz"

    log_info "Starting $BACKUP_TYPE backup for $ENVIRONMENT environment..."
    log_info "Backup file: $compressed_backup"

    # Calculate database size for progress indication
    local db_size=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT pg_size_pretty(pg_database_size('$DB_NAME'));" | xargs)
    log_info "Database size: $db_size"

    # Perform backup based on type
    case "$BACKUP_TYPE" in
        "full")
            perform_full_backup "$backup_file"
            ;;
        "schema")
            perform_schema_backup "$backup_file"
            ;;
        "data")
            perform_data_backup "$backup_file"
            ;;
        *)
            log_error "Unknown backup type: $BACKUP_TYPE"
            exit 1
            ;;
    esac

    # Compress the backup
    log_info "Compressing backup..."
    gzip -"$COMPRESSION_LEVEL" "$backup_file"

    # Verify backup integrity
    verify_backup_integrity "$compressed_backup"

    # Generate backup metadata
    create_backup_metadata "$compressed_backup" "$db_size"

    # Upload to cloud storage if configured
    upload_to_cloud_storage "$compressed_backup"

    # Encrypt backup if GPG is available
    encrypt_backup "$compressed_backup"

    log_success "Backup completed successfully: $compressed_backup"
    return 0
}

# Function to perform full database backup
perform_full_backup() {
    local backup_file="$1"

    log_info "Performing full database backup..."

    PGPASSWORD="$DB_PASSWORD" pg_dump \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --username="$DB_USER" \
        --dbname="$DB_NAME" \
        --verbose \
        --no-password \
        --format=plain \
        --no-owner \
        --no-privileges \
        --create \
        --clean \
        --if-exists \
        --quote-all-identifiers \
        --no-tablespaces \
        --no-unlogged-table-data \
        --serializable-deferrable \
        --lock-wait-timeout=30000 \
        --file="$backup_file" 2>> "$LOG_FILE"

    log_success "Full backup completed"
}

# Function to perform schema-only backup
perform_schema_backup() {
    local backup_file="$1"

    log_info "Performing schema-only backup..."

    PGPASSWORD="$DB_PASSWORD" pg_dump \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --username="$DB_USER" \
        --dbname="$DB_NAME" \
        --verbose \
        --no-password \
        --format=plain \
        --schema-only \
        --no-owner \
        --no-privileges \
        --create \
        --clean \
        --if-exists \
        --quote-all-identifiers \
        --file="$backup_file" 2>> "$LOG_FILE"

    log_success "Schema backup completed"
}

# Function to perform data-only backup
perform_data_backup() {
    local backup_file="$1"

    log_info "Performing data-only backup..."

    PGPASSWORD="$DB_PASSWORD" pg_dump \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --username="$DB_USER" \
        --dbname="$DB_NAME" \
        --verbose \
        --no-password \
        --format=plain \
        --data-only \
        --no-owner \
        --no-privileges \
        --quote-all-identifiers \
        --disable-triggers \
        --file="$backup_file" 2>> "$LOG_FILE"

    log_success "Data backup completed"
}

# Function to verify backup integrity
verify_backup_integrity() {
    local compressed_backup="$1"

    log_info "Verifying backup integrity..."

    # Test gzip integrity
    if gzip -t "$compressed_backup"; then
        log_success "Backup compression integrity verified"
    else
        log_error "Backup compression integrity check failed"
        exit 1
    fi

    # Check if backup file is not empty
    if [[ -s "$compressed_backup" ]]; then
        local backup_size=$(du -h "$compressed_backup" | cut -f1)
        log_success "Backup file size: $backup_size"
    else
        log_error "Backup file is empty"
        exit 1
    fi

    # Quick SQL syntax validation
    if gzip -dc "$compressed_backup" | head -100 | grep -q "PostgreSQL database dump"; then
        log_success "Backup file format verified"
    else
        log_warning "Could not verify backup file format"
    fi
}

# Function to create backup metadata
create_backup_metadata() {
    local compressed_backup="$1"
    local db_size="$2"
    local metadata_file="${compressed_backup}.meta.json"

    log_info "Creating backup metadata..."

    # Calculate file checksums
    local md5_checksum=$(md5sum "$compressed_backup" | cut -d' ' -f1)
    local sha256_checksum=$(sha256sum "$compressed_backup" | cut -d' ' -f1)
    local backup_size=$(du -b "$compressed_backup" | cut -f1)

    # Generate metadata JSON
    cat > "$metadata_file" << EOF
{
    "backup_info": {
        "filename": "$(basename "$compressed_backup")",
        "environment": "$ENVIRONMENT",
        "backup_type": "$BACKUP_TYPE",
        "timestamp": "$TIMESTAMP",
        "created_at": "$(date -u '+%Y-%m-%d %H:%M:%S UTC')",
        "database": {
            "host": "$DB_HOST",
            "port": $DB_PORT,
            "name": "$DB_NAME",
            "user": "$DB_USER",
            "size": "$db_size"
        },
        "backup_file": {
            "size_bytes": $backup_size,
            "size_human": "$(du -h "$compressed_backup" | cut -f1)",
            "compression_level": $COMPRESSION_LEVEL,
            "md5_checksum": "$md5_checksum",
            "sha256_checksum": "$sha256_checksum"
        },
        "backup_config": {
            "retention_days": $BACKUP_RETENTION_DAYS,
            "script_version": "1.0.0"
        }
    }
}
EOF

    log_success "Backup metadata created: $metadata_file"
}

# Function to upload backup to cloud storage
upload_to_cloud_storage() {
    local compressed_backup="$1"

    if [[ -n "${AWS_S3_BUCKET:-}" ]] && command -v aws >/dev/null 2>&1; then
        log_info "Uploading backup to S3..."

        local s3_key="database-backups/$ENVIRONMENT/$(basename "$compressed_backup")"
        local metadata_file="${compressed_backup}.meta.json"

        # Upload backup file
        if aws s3 cp "$compressed_backup" "s3://$AWS_S3_BUCKET/$s3_key" \
            --storage-class STANDARD_IA \
            --metadata environment="$ENVIRONMENT",backup-type="$BACKUP_TYPE",timestamp="$TIMESTAMP"; then
            log_success "Backup uploaded to S3: s3://$AWS_S3_BUCKET/$s3_key"

            # Upload metadata file
            aws s3 cp "$metadata_file" "s3://$AWS_S3_BUCKET/${s3_key}.meta.json" \
                --content-type application/json || log_warning "Failed to upload metadata to S3"
        else
            log_warning "Failed to upload backup to S3"
        fi
    else
        log_info "S3 upload not configured or AWS CLI not available"
    fi
}

# Function to encrypt backup
encrypt_backup() {
    local compressed_backup="$1"

    if [[ -n "${GPG_RECIPIENT:-}" ]] && command -v gpg >/dev/null 2>&1; then
        log_info "Encrypting backup with GPG..."

        local encrypted_backup="${compressed_backup}.gpg"

        if gpg --trust-model always --encrypt --recipient "$GPG_RECIPIENT" \
            --output "$encrypted_backup" "$compressed_backup"; then
            log_success "Backup encrypted: $encrypted_backup"

            # Remove unencrypted backup if encryption succeeded
            if [[ "${REMOVE_UNENCRYPTED:-false}" == "true" ]]; then
                rm -f "$compressed_backup"
                log_info "Unencrypted backup removed"
            fi
        else
            log_warning "Failed to encrypt backup"
        fi
    else
        log_info "GPG encryption not configured or GPG not available"
    fi
}

# Function to cleanup old backups
cleanup_old_backups() {
    log_info "Cleaning up old backups (retention: $BACKUP_RETENTION_DAYS days)..."

    # Find and delete old backup files
    local deleted_count=0
    while IFS= read -r -d '' old_backup; do
        log_info "Removing old backup: $(basename "$old_backup")"
        rm -f "$old_backup" "${old_backup}.meta.json" "${old_backup}.gpg"
        ((deleted_count++))
    done < <(find "$BACKUP_DIR" -name "${ENVIRONMENT}_*.sql.gz" -type f -mtime +$BACKUP_RETENTION_DAYS -print0)

    if [[ $deleted_count -gt 0 ]]; then
        log_success "Cleaned up $deleted_count old backup files"
    else
        log_info "No old backup files to clean up"
    fi

    # Clean up old S3 backups if configured
    if [[ -n "${AWS_S3_BUCKET:-}" ]] && command -v aws >/dev/null 2>&1; then
        log_info "Cleaning up old S3 backups..."

        # List and delete old S3 objects
        local cutoff_date=$(date -d "$BACKUP_RETENTION_DAYS days ago" '+%Y-%m-%d')
        aws s3api list-objects-v2 \
            --bucket "$AWS_S3_BUCKET" \
            --prefix "database-backups/$ENVIRONMENT/" \
            --query "Contents[?LastModified<=\`$cutoff_date\`].Key" \
            --output text | while read -r key; do
                if [[ -n "$key" && "$key" != "None" ]]; then
                    aws s3 rm "s3://$AWS_S3_BUCKET/$key"
                    log_info "Removed old S3 backup: $key"
                fi
            done
    fi
}

# Function to generate backup report
generate_backup_report() {
    log_info "Generating backup report..."

    local report_file="${BACKUP_DIR}/backup_report_${TIMESTAMP}.html"

    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Database Backup Report - $ENVIRONMENT</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f8ff; padding: 20px; border-radius: 5px; }
        .success { color: green; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
        .error { color: red; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Database Backup Report</h1>
        <p><strong>Environment:</strong> $ENVIRONMENT</p>
        <p><strong>Backup Type:</strong> $BACKUP_TYPE</p>
        <p><strong>Timestamp:</strong> $(date -u '+%Y-%m-%d %H:%M:%S UTC')</p>
        <p><strong>Database:</strong> $DB_NAME@$DB_HOST:$DB_PORT</p>
    </div>

    <h2>Backup Files</h2>
    <table>
        <thead>
            <tr>
                <th>Filename</th>
                <th>Size</th>
                <th>Created</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
EOF

    # List recent backup files
    find "$BACKUP_DIR" -name "${ENVIRONMENT}_*.sql.gz" -type f -mtime -1 | while read -r backup_file; do
        local filename=$(basename "$backup_file")
        local size=$(du -h "$backup_file" | cut -f1)
        local created=$(date -r "$backup_file" '+%Y-%m-%d %H:%M:%S')

        echo "            <tr>" >> "$report_file"
        echo "                <td>$filename</td>" >> "$report_file"
        echo "                <td>$size</td>" >> "$report_file"
        echo "                <td>$created</td>" >> "$report_file"
        echo "                <td class=\"success\">✓ Success</td>" >> "$report_file"
        echo "            </tr>" >> "$report_file"
    done

    cat >> "$report_file" << EOF
        </tbody>
    </table>

    <h2>Backup Log (Last 50 lines)</h2>
    <pre style="background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto;">
$(tail -50 "$LOG_FILE" | sed 's/\x1b\[[0-9;]*m//g')
    </pre>
</body>
</html>
EOF

    log_success "Backup report generated: $report_file"
}

# Main function
main() {
    log_info "=== Database Backup Script Started ==="
    log_info "Environment: $ENVIRONMENT"
    log_info "Backup Type: $BACKUP_TYPE"
    log_info "Timestamp: $TIMESTAMP"

    # Validate prerequisites
    validate_prerequisites

    # Perform backup
    if perform_backup; then
        # Cleanup old backups
        cleanup_old_backups

        # Generate report
        generate_backup_report

        # Send success notification
        send_notification "SUCCESS" "Database backup completed successfully"

        log_success "=== Database Backup Script Completed Successfully ==="
    else
        log_error "=== Database Backup Script Failed ==="
        exit 1
    fi
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi