package com.platform.audit.internal;

import com.platform.audit.api.AuditLogDetailDTO;
import com.platform.audit.api.AuditLogEntryDTO;
import com.platform.audit.api.AuditLogSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for audit log viewing with proper permission filtering.
 * Handles all business logic for audit log access, filtering, and permission enforcement.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogViewService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogViewService.class);

    private final AuditEventRepository auditEventRepository;
    private final UserPermissionScopeService permissionScopeService;

    public AuditLogViewService(AuditEventRepository auditEventRepository,
                              UserPermissionScopeService permissionScopeService) {
        this.auditEventRepository = auditEventRepository;
        this.permissionScopeService = permissionScopeService;
    }

    /**
     * Get audit logs for a user with proper permission filtering.
     */
    public AuditLogSearchResponse getAuditLogs(UUID userId, AuditLogFilter filter) {
        log.debug("Getting audit logs for user: {} with filter: {}", userId, filter);

        // Apply permission-based filtering
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, filter);

        // Create pageable with sort by timestamp descending
        Pageable pageable = PageRequest.of(
            scopedFilter.pageNumber(),
            scopedFilter.pageSize(),
            Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // Query audit events with filters
        Page<AuditEvent> auditPage = queryAuditEvents(scopedFilter, pageable);

        // Convert to DTOs with proper redaction
        List<AuditLogEntryDTO> entries = auditPage.getContent().stream()
            .map(event -> permissionScopeService.applyDataRedaction(userId, event))
            .toList();

        log.debug("Retrieved {} audit log entries for user: {}", entries.size(), userId);

        return new AuditLogSearchResponse(
            entries,
            auditPage.getNumber(),
            auditPage.getSize(),
            auditPage.getTotalElements(),
            auditPage.getTotalPages(),
            auditPage.isFirst(),
            auditPage.isLast(),
            scopedFilter.hasSearch(),
            scopedFilter.hasDateRange()
        );
    }

    /**
     * Get detailed view of a specific audit log entry.
     */
    public Optional<AuditLogDetailDTO> getAuditLogDetail(UUID userId, UUID auditLogId) {
        log.debug("Getting audit log detail for user: {} and entry: {}", userId, auditLogId);

        Optional<AuditEvent> auditEventOpt = auditEventRepository.findById(auditLogId);
        if (auditEventOpt.isEmpty()) {
            log.debug("Audit log entry not found: {}", auditLogId);
            return Optional.empty();
        }

        AuditEvent auditEvent = auditEventOpt.get();

        // Check if user has access to this specific entry
        if (!permissionScopeService.canAccessAuditEntry(userId, auditEvent)) {
            log.warn("User {} attempted to access unauthorized audit entry: {}", userId, auditLogId);
            return Optional.empty();
        }

        // Apply data redaction based on user permissions
        AuditLogEntryDTO redactedEntry = permissionScopeService.applyDataRedaction(userId, auditEvent);

        // Convert to detailed DTO
        AuditLogDetailDTO detailDTO = AuditLogDetailDTO.fromAuditEvent(auditEvent, redactedEntry);

        log.debug("Retrieved audit log detail for user: {} and entry: {}", userId, auditLogId);
        return Optional.of(detailDTO);
    }

    /**
     * Query audit events with filtering and pagination.
     */
    private Page<AuditEvent> queryAuditEvents(AuditLogFilter filter, Pageable pageable) {
        if (filter.hasSearch()) {
            // Use search functionality
            return searchAuditEvents(filter, pageable);
        } else {
            // Use basic filtering
            return filterAuditEvents(filter, pageable);
        }
    }

    /**
     * Filter audit events by basic criteria.
     */
    private Page<AuditEvent> filterAuditEvents(AuditLogFilter filter, Pageable pageable) {
        if (filter.userId() != null) {
            // User-specific filtering
            return auditEventRepository.findByOrganizationIdAndActorIdAndTimestampBetween(
                filter.organizationId(),
                filter.userId(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        } else {
            // Organization-wide filtering
            return auditEventRepository.findByOrganizationIdAndTimestampBetween(
                filter.organizationId(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        }
    }

    /**
     * Search audit events with full-text search.
     */
    private Page<AuditEvent> searchAuditEvents(AuditLogFilter filter, Pageable pageable) {
        String searchTerm = filter.getSearchTerm();

        if (filter.userId() != null) {
            // User-specific search
            return auditEventRepository.searchByOrganizationAndUser(
                filter.organizationId(),
                filter.userId(),
                searchTerm,
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        } else {
            // Organization-wide search
            return auditEventRepository.searchByOrganization(
                filter.organizationId(),
                searchTerm,
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        }
    }

    /**
     * Get statistics about audit logs for a user.
     */
    public AuditLogStatistics getAuditLogStatistics(UUID userId, Instant dateFrom, Instant dateTo) {
        log.debug("Getting audit log statistics for user: {} from {} to {}", userId, dateFrom, dateTo);

        // Create a basic filter for statistics
        AuditLogFilter filter = AuditLogFilter.forUser(userId, null, dateFrom, dateTo, null, 0, 1);
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, filter);

        // Count total entries in date range
        long totalEntries = auditEventRepository.countByOrganizationIdAndTimestampBetween(
            scopedFilter.organizationId(),
            dateFrom != null ? dateFrom : Instant.EPOCH,
            dateTo != null ? dateTo : Instant.now()
        );

        // Get most recent activity
        Optional<AuditEvent> lastActivity = auditEventRepository
            .findFirstByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
                scopedFilter.organizationId(),
                dateFrom != null ? dateFrom : Instant.EPOCH,
                dateTo != null ? dateTo : Instant.now()
            );

        return new AuditLogStatistics(
            totalEntries,
            lastActivity.map(AuditEvent::getTimestamp).orElse(null),
            dateFrom,
            dateTo
        );
    }

    /**
     * Statistics record for audit log data.
     */
    public record AuditLogStatistics(
        long totalEntries,
        Instant lastActivity,
        Instant periodStart,
        Instant periodEnd
    ) {}
}