package com.platform.audit.internal;

import com.platform.audit.api.dto.AuditLogDetailDTO;
import com.platform.audit.api.dto.AuditLogEntryDTO;
import com.platform.audit.internal.AuditLogPermissionService.UserAuditPermissions;
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
 * Service for user-facing audit log viewing operations.
 *
 * This service provides secure access to audit logs with proper permission
 * checking, tenant isolation, and data transformation for user consumption.
 * It implements business logic for the user-facing audit log viewer feature.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogViewService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogViewService.class);

    private final AuditLogViewRepository auditLogViewRepository;
    private final AuditLogPermissionService permissionService;

    public AuditLogViewService(
            AuditLogViewRepository auditLogViewRepository,
            AuditLogPermissionService permissionService) {
        this.auditLogViewRepository = auditLogViewRepository;
        this.permissionService = permissionService;
    }

    /**
     * Retrieve audit logs with filtering and pagination.
     *
     * @param userId the user requesting the logs
     * @param filter the filter criteria
     * @return search response with audit logs and pagination info
     */
    public AuditLogSearchResponse getAuditLogs(UUID userId, AuditLogFilter filter) {
        log.debug("Getting audit logs for user: {} with filter: {}", userId, filter);

        // Get user's organization and permissions
        var permissions = permissionService.getUserAuditPermissions(userId);
        if (!permissions.canViewAuditLogs()) {
            throw new SecurityException("User does not have permission to view audit logs");
        }

        // Create pageable with sorting
        Sort sort = createSort(filter.sortField(), filter.sortDirection());
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), sort);

        // Apply tenant isolation - only show logs from user's organization
        UUID organizationId = permissions.organizationId();
        boolean includeSystemActions = permissions.canViewSystemActions();

        Page<AuditEvent> events;

        // Apply filters based on what's provided
        if (hasComplexFilters(filter)) {
            events = auditLogViewRepository.findUserAccessibleLogsWithFilters(
                organizationId,
                userId,
                includeSystemActions,
                filter.searchText(),
                filter.actionTypes(),
                filter.resourceTypes(),
                filter.dateFrom(),
                filter.dateTo(),
                pageable
            );
        } else if (filter.dateFrom() != null || filter.dateTo() != null) {
            Instant startDate = filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH;
            Instant endDate = filter.dateTo() != null ? filter.dateTo() : Instant.now();

            events = auditLogViewRepository.findUserAccessibleLogsInDateRange(
                organizationId,
                userId,
                includeSystemActions,
                startDate,
                endDate,
                pageable
            );
        } else {
            events = auditLogViewRepository.findUserAccessibleLogs(
                organizationId,
                userId,
                includeSystemActions,
                pageable
            );
        }

        // Convert to DTOs
        List<AuditLogEntryDTO> entries = events.getContent().stream()
            .map(this::convertToEntryDTO)
            .toList();

        log.debug("Found {} audit log entries for user: {}", entries.size(), userId);

        return new AuditLogSearchResponse(
            entries,
            events.getNumber(),
            events.getSize(),
            events.getTotalElements()
        );
    }

    /**
     * Get detailed information for a specific audit log entry.
     *
     * @param userId the user requesting the detail
     * @param entryId the audit log entry ID
     * @return detailed audit log information if accessible
     */
    public Optional<AuditLogDetailDTO> getAuditLogDetail(UUID userId, UUID entryId) {
        log.debug("Getting audit log detail for user: {} and entry: {}", userId, entryId);

        // Check permissions
        var permissions = permissionService.getUserAuditPermissions(userId);
        if (!permissions.canViewAuditLogs()) {
            throw new SecurityException("User does not have permission to view audit logs");
        }

        UUID organizationId = permissions.organizationId();
        boolean includeSystemActions = permissions.canViewSystemActions();

        // Find the specific entry with permission check
        Optional<AuditEvent> eventOpt = auditLogViewRepository.findUserAccessibleLogById(
            entryId,
            organizationId,
            userId,
            includeSystemActions
        );

        if (eventOpt.isEmpty()) {
            log.debug("Audit log entry not found or not accessible: {} for user: {}", entryId, userId);
            return Optional.empty();
        }

        AuditEvent event = eventOpt.get();

        // Apply data redaction based on sensitivity and user permissions
        AuditLogDetailDTO detail = convertToDetailDTO(event, permissions);

        log.debug("Retrieved audit log detail for entry: {} and user: {}", entryId, userId);
        return Optional.of(detail);
    }

    /**
     * Convert AuditEvent to AuditLogEntryDTO for API consumption.
     */
    private AuditLogEntryDTO convertToEntryDTO(AuditEvent event) {
        // Extract user agent from details if available
        String userAgent = null;
        if (event.getDetails().containsKey("userAgent")) {
            userAgent = event.getDetails().get("userAgent").toString();
        }

        return new AuditLogEntryDTO(
            event.getId().toString(),
            event.getCreatedAt(),
            resolveActorDisplayName(event),
            determineActorType(event),
            mapToActionType(event.getAction()),
            generateActionDescription(event),
            mapToResourceType(event.getResourceType()),
            resolveResourceDisplayName(event),
            determineOutcome(event),
            determineSensitivity(event)
        );
    }

    /**
     * Convert AuditEvent to AuditLogDetailDTO with sensitive data handling.
     */
    private AuditLogDetailDTO convertToDetailDTO(AuditEvent event, UserAuditPermissions permissions) {
        // Start with basic entry data
        AuditLogEntryDTO basicEntry = convertToEntryDTO(event);

        // Extract additional details based on permissions
        String actorId = permissions.canViewSensitiveData() ?
            event.getActorId().toString() : null;

        String resourceId = permissions.canViewSensitiveData() && event.getResourceId() != null ?
            event.getResourceId().toString() : null;

        String ipAddress = permissions.canViewTechnicalData() ?
            event.getIpAddress() : null;

        String userAgent = null;
        if (permissions.canViewTechnicalData() && event.getDetails().containsKey("userAgent")) {
            userAgent = event.getDetails().get("userAgent").toString();
        }

        // Filter additional data based on permissions
        var additionalData = filterAdditionalData(event.getDetails(), permissions);

        return AuditLogDetailDTO.fromBasicEntry(
            basicEntry,
            actorId,
            resourceId,
            ipAddress,
            userAgent,
            additionalData
        );
    }

    // Helper methods

    private boolean hasComplexFilters(AuditLogFilter filter) {
        return filter.searchText() != null ||
               (filter.actionTypes() != null && !filter.actionTypes().isEmpty()) ||
               (filter.resourceTypes() != null && !filter.resourceTypes().isEmpty()) ||
               filter.dateFrom() != null ||
               filter.dateTo() != null;
    }

    private Sort createSort(String sortField, String direction) {
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ?
            Sort.Direction.ASC : Sort.Direction.DESC;

        // Map frontend field names to entity field names
        String entityField = switch (sortField) {
            case "timestamp" -> "createdAt";
            case "actionType" -> "action";
            case "resourceType" -> "resourceType";
            default -> "createdAt";
        };

        return Sort.by(sortDirection, entityField);
    }

    private String determineActorType(AuditEvent event) {
        if (event.getActorId() == null) {
            return AuditLogEntryDTO.ActorType.SYSTEM;
        }

        // This would typically check against user/service account tables
        // For now, assume all non-null actorIds are users
        return AuditLogEntryDTO.ActorType.USER;
    }

    private String resolveActorDisplayName(AuditEvent event) {
        if (event.getActorId() == null) {
            return "System";
        }

        // TODO: Implement user name resolution
        // This would typically query the user service or cache
        return "User " + event.getActorId().toString().substring(0, 8);
    }

    private String mapToActionType(String action) {
        if (action == null) return AuditLogEntryDTO.ActionType.VIEW;

        return switch (action.toLowerCase()) {
            case "user.login", "auth.login" -> AuditLogEntryDTO.ActionType.LOGIN;
            case "user.logout", "auth.logout" -> AuditLogEntryDTO.ActionType.LOGOUT;
            case "data.created", "resource.created" -> AuditLogEntryDTO.ActionType.CREATE;
            case "data.updated", "resource.updated" -> AuditLogEntryDTO.ActionType.UPDATE;
            case "data.deleted", "resource.deleted" -> AuditLogEntryDTO.ActionType.DELETE;
            case "data.exported", "export.created" -> AuditLogEntryDTO.ActionType.EXPORT;
            case "payment.processed", "payment.created" -> AuditLogEntryDTO.ActionType.PAYMENT_CREATED;
            case "subscription.created" -> AuditLogEntryDTO.ActionType.SUBSCRIPTION_CREATED;
            case "subscription.updated", "subscription.modified" -> AuditLogEntryDTO.ActionType.SUBSCRIPTION_MODIFIED;
            default -> AuditLogEntryDTO.ActionType.VIEW;
        };
    }

    private String generateActionDescription(AuditEvent event) {
        String action = event.getAction();
        String resourceType = event.getResourceType();

        if (action == null) return "Unknown action";

        // Generate human-readable descriptions
        return switch (action.toLowerCase()) {
            case "user.login" -> "User logged in";
            case "user.logout" -> "User logged out";
            case "data.created" -> "Created " + (resourceType != null ? resourceType : "resource");
            case "data.updated" -> "Updated " + (resourceType != null ? resourceType : "resource");
            case "data.deleted" -> "Deleted " + (resourceType != null ? resourceType : "resource");
            case "data.exported" -> "Exported " + (resourceType != null ? resourceType : "data");
            case "payment.processed" -> "Processed payment";
            case "subscription.created" -> "Created subscription";
            case "subscription.updated" -> "Updated subscription";
            default -> action.replace(".", " ").replace("_", " ");
        };
    }

    private String mapToResourceType(String resourceType) {
        if (resourceType == null) return null;

        return switch (resourceType.toLowerCase()) {
            case "user" -> AuditLogEntryDTO.ResourceType.USER;
            case "organization" -> AuditLogEntryDTO.ResourceType.ORGANIZATION;
            case "payment" -> AuditLogEntryDTO.ResourceType.PAYMENT;
            case "subscription" -> AuditLogEntryDTO.ResourceType.SUBSCRIPTION;
            case "audit", "audit_log" -> AuditLogEntryDTO.ResourceType.AUDIT_LOG;
            case "auth", "authentication" -> AuditLogEntryDTO.ResourceType.AUTHENTICATION;
            default -> null;
        };
    }

    private String resolveResourceDisplayName(AuditEvent event) {
        if (event.getResourceType() == null) return null;

        // TODO: Implement resource name resolution
        // This would typically query the appropriate service
        if (event.getResourceId() != null) {
            return event.getResourceType() + " " + event.getResourceId().toString().substring(0, 8);
        }

        return event.getResourceType();
    }

    private String determineOutcome(AuditEvent event) {
        // Check details for failure indicators
        var details = event.getDetails();

        if (details.containsKey("error") || details.containsKey("failure") ||
            event.getAction().contains("failed")) {
            return AuditLogEntryDTO.Outcome.FAILURE;
        }

        if (details.containsKey("partial") || details.containsKey("warning")) {
            return AuditLogEntryDTO.Outcome.PARTIAL;
        }

        return AuditLogEntryDTO.Outcome.SUCCESS;
    }

    private String determineSensitivity(AuditEvent event) {
        String action = event.getAction();
        String resourceType = event.getResourceType();

        // Determine sensitivity based on action and resource type
        if (isHighSensitivityAction(action) || isHighSensitivityResource(resourceType)) {
            return AuditLogEntryDTO.SensitivityLevel.CONFIDENTIAL;
        }

        if (isMediumSensitivityAction(action) || isMediumSensitivityResource(resourceType)) {
            return AuditLogEntryDTO.SensitivityLevel.INTERNAL;
        }

        return AuditLogEntryDTO.SensitivityLevel.PUBLIC;
    }

    private boolean isHighSensitivityAction(String action) {
        if (action == null) return false;
        return action.contains("payment") || action.contains("delete") ||
               action.contains("export") || action.contains("admin");
    }

    private boolean isHighSensitivityResource(String resourceType) {
        if (resourceType == null) return false;
        return resourceType.equals("payment") || resourceType.equals("financial");
    }

    private boolean isMediumSensitivityAction(String action) {
        if (action == null) return false;
        return action.contains("update") || action.contains("create") ||
               action.contains("subscription");
    }

    private boolean isMediumSensitivityResource(String resourceType) {
        if (resourceType == null) return false;
        return resourceType.equals("user") || resourceType.equals("subscription");
    }

    private java.util.Map<String, Object> filterAdditionalData(
            java.util.Map<String, Object> details,
            UserAuditPermissions permissions) {

        if (details.isEmpty() || !permissions.canViewSensitiveData()) {
            return java.util.Map.of();
        }

        // Filter out sensitive keys based on permissions
        return details.entrySet().stream()
            .filter(entry -> !isSensitiveKey(entry.getKey()) || permissions.canViewSensitiveData())
            .collect(java.util.stream.Collectors.toMap(
                java.util.Map.Entry::getKey,
                java.util.Map.Entry::getValue
            ));
    }

    private boolean isSensitiveKey(String key) {
        return key.toLowerCase().contains("password") ||
               key.toLowerCase().contains("token") ||
               key.toLowerCase().contains("secret") ||
               key.toLowerCase().contains("key");
    }

    // Record classes for internal use

    public record AuditLogSearchResponse(
        List<AuditLogEntryDTO> entries,
        int pageNumber,
        int pageSize,
        long totalElements
    ) {}
}