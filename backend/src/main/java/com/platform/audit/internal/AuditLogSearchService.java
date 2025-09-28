package com.platform.audit.internal;

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
import java.util.UUID;

/**
 * Specialized service for audit log search functionality.
 * Handles advanced search features including full-text search, faceted search,
 * and search result highlighting.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogSearchService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSearchService.class);

    private final AuditEventRepository auditEventRepository;
    private final UserPermissionScopeService permissionScopeService;

    public AuditLogSearchService(AuditEventRepository auditEventRepository,
                               UserPermissionScopeService permissionScopeService) {
        this.auditEventRepository = auditEventRepository;
        this.permissionScopeService = permissionScopeService;
    }

    /**
     * Perform advanced search across audit logs with highlighting and faceting.
     */
    public AuditLogSearchResponse advancedSearch(UUID userId, AdvancedSearchCriteria criteria) {
        log.debug("Performing advanced search for user: {} with criteria: {}", userId, criteria);

        // Create filter from search criteria
        AuditLogFilter filter = createFilterFromCriteria(criteria);

        // Apply permission-based filtering
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, filter);

        // Create pageable with sort
        Pageable pageable = PageRequest.of(
            criteria.pageNumber(),
            criteria.pageSize(),
            createSort(criteria.sortBy(), criteria.sortDirection())
        );

        // Perform the search
        Page<AuditEvent> searchResults = performSearch(scopedFilter, pageable);

        // Convert to DTOs with highlighting if requested
        List<AuditLogEntryDTO> entries = searchResults.getContent().stream()
            .map(event -> {
                var dto = permissionScopeService.applyDataRedaction(userId, event);
                if (criteria.highlightMatches() && criteria.searchTerm() != null) {
                    return addSearchHighlighting(dto, criteria.searchTerm());
                }
                return dto;
            })
            .toList();

        // Build search facets if requested
        SearchFacets facets = criteria.includeFacets() ?
            buildSearchFacets(scopedFilter) : null;

        log.debug("Advanced search returned {} results for user: {}", entries.size(), userId);

        return new AuditLogSearchResponse(
            entries,
            searchResults.getNumber(),
            searchResults.getSize(),
            searchResults.getTotalElements(),
            searchResults.getTotalPages(),
            searchResults.isFirst(),
            searchResults.isLast(),
            true, // hasSearch
            scopedFilter.hasDateRange(),
            facets,
            criteria.searchTerm()
        );
    }

    /**
     * Quick search with minimal processing for fast results.
     */
    public AuditLogSearchResponse quickSearch(UUID userId, String searchTerm,
                                            Instant dateFrom, Instant dateTo,
                                            int page, int size) {
        log.debug("Performing quick search for user: {} with term: '{}'", userId, searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty for quick search");
        }

        AuditLogFilter filter = AuditLogFilter.forUser(userId, null, dateFrom, dateTo, searchTerm, page, size);
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, filter);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditEvent> searchResults = auditEventRepository.searchByOrganization(
            scopedFilter.organizationId(),
            scopedFilter.getSearchTerm(),
            dateFrom != null ? dateFrom : Instant.EPOCH,
            dateTo != null ? dateTo : Instant.now(),
            pageable
        );

        List<AuditLogEntryDTO> entries = searchResults.getContent().stream()
            .map(event -> permissionScopeService.applyDataRedaction(userId, event))
            .toList();

        return new AuditLogSearchResponse(
            entries,
            searchResults.getNumber(),
            searchResults.getSize(),
            searchResults.getTotalElements(),
            searchResults.getTotalPages(),
            searchResults.isFirst(),
            searchResults.isLast(),
            true,
            dateFrom != null || dateTo != null,
            null,
            searchTerm
        );
    }

    /**
     * Get search suggestions based on partial input.
     */
    public List<String> getSearchSuggestions(UUID userId, String partialTerm, int maxSuggestions) {
        log.debug("Getting search suggestions for user: {} with partial term: '{}'", userId, partialTerm);

        if (partialTerm == null || partialTerm.trim().length() < 2) {
            return List.of();
        }

        // Get user's organization for scope
        AuditLogFilter basicFilter = AuditLogFilter.forUser(userId, null, null, null, null, 0, 1);
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, basicFilter);

        // Get suggestions from action types
        List<String> actionSuggestions = auditEventRepository
            .findDistinctActionTypesContaining(scopedFilter.organizationId(), partialTerm.toLowerCase(), maxSuggestions);

        // Get suggestions from actor names (if user has permission)
        List<String> actorSuggestions = auditEventRepository
            .findDistinctActorNamesContaining(scopedFilter.organizationId(), partialTerm.toLowerCase(), maxSuggestions / 2);

        // Combine and return
        return List.of(actionSuggestions, actorSuggestions).stream()
            .flatMap(List::stream)
            .distinct()
            .limit(maxSuggestions)
            .toList();
    }

    /**
     * Create filter from advanced search criteria.
     */
    private AuditLogFilter createFilterFromCriteria(AdvancedSearchCriteria criteria) {
        return new AuditLogFilter(
            null, // Will be set by permission service
            null, // Will be set by permission service
            criteria.dateFrom(),
            criteria.dateTo(),
            criteria.searchTerm(),
            criteria.actionTypes(),
            criteria.actorEmails(),
            criteria.includeSystemActions(),
            criteria.pageNumber(),
            criteria.pageSize()
        );
    }

    /**
     * Create sort specification from criteria.
     */
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ?
            Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy != null ? sortBy.toLowerCase() : "timestamp") {
            case "actor" -> Sort.by(direction, "actorName", "timestamp");
            case "action" -> Sort.by(direction, "actionType", "timestamp");
            case "resource" -> Sort.by(direction, "resourceType", "timestamp");
            default -> Sort.by(direction, "timestamp");
        };
    }

    /**
     * Perform the actual search based on filter criteria.
     */
    private Page<AuditEvent> performSearch(AuditLogFilter filter, Pageable pageable) {
        if (filter.hasSearch()) {
            return auditEventRepository.searchByOrganization(
                filter.organizationId(),
                filter.getSearchTerm(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        } else {
            return auditEventRepository.findByOrganizationIdAndTimestampBetween(
                filter.organizationId(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now(),
                pageable
            );
        }
    }

    /**
     * Add search term highlighting to DTO fields.
     */
    private AuditLogEntryDTO addSearchHighlighting(AuditLogEntryDTO dto, String searchTerm) {
        String highlightTag = "<mark>";
        String highlightEndTag = "</mark>";
        String lowerSearchTerm = searchTerm.toLowerCase();

        // Helper method to add highlighting
        String addHighlight = (String text) -> {
            if (text == null) return null;
            return text.replaceAll("(?i)" + lowerSearchTerm,
                highlightTag + lowerSearchTerm + highlightEndTag);
        };

        // Create highlighted version
        return new AuditLogEntryDTO(
            dto.id(),
            dto.timestamp(),
            addHighlight.apply(dto.actorName()),
            dto.actorEmail(), // Don't highlight email for security
            addHighlight.apply(dto.actionType()),
            addHighlight.apply(dto.resourceType()),
            dto.resourceId(),
            addHighlight.apply(dto.description()),
            dto.outcome(),
            dto.ipAddress(),
            dto.userAgent(),
            dto.sessionId(),
            dto.correlationId()
        );
    }

    /**
     * Build search facets for filtering options.
     */
    private SearchFacets buildSearchFacets(AuditLogFilter filter) {
        // Get action type facets
        List<FacetItem> actionTypeFacets = auditEventRepository
            .getActionTypeFacets(filter.organizationId(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now())
            .stream()
            .map(result -> new FacetItem(result.getValue(), result.getCount()))
            .toList();

        // Get outcome facets
        List<FacetItem> outcomeFacets = auditEventRepository
            .getOutcomeFacets(filter.organizationId(),
                filter.dateFrom() != null ? filter.dateFrom() : Instant.EPOCH,
                filter.dateTo() != null ? filter.dateTo() : Instant.now())
            .stream()
            .map(result -> new FacetItem(result.getValue(), result.getCount()))
            .toList();

        return new SearchFacets(actionTypeFacets, outcomeFacets);
    }

    /**
     * Advanced search criteria record.
     */
    public record AdvancedSearchCriteria(
        String searchTerm,
        Instant dateFrom,
        Instant dateTo,
        List<String> actionTypes,
        List<String> actorEmails,
        Boolean includeSystemActions,
        String sortBy,
        String sortDirection,
        boolean highlightMatches,
        boolean includeFacets,
        int pageNumber,
        int pageSize
    ) {}

    /**
     * Search facets for filtering.
     */
    public record SearchFacets(
        List<FacetItem> actionTypes,
        List<FacetItem> outcomes
    ) {}

    /**
     * Individual facet item.
     */
    public record FacetItem(
        String value,
        long count
    ) {}
}