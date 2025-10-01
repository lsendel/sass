package com.platform.user.api;

import com.platform.user.Organization;
import com.platform.user.api.dto.CreateOrganizationRequest;
import com.platform.user.internal.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for organization operations.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(final OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Creates a new organization.
     *
     * @param request the create request
     * @return the created organization
     */
    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @Valid @RequestBody final CreateOrganizationRequest request) {
        final Organization organization = organizationService.createOrganization(
            request.name(),
            request.slug()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }

    /**
     * Gets an organization by ID.
     *
     * @param id the organization ID
     * @return the organization
     */
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable final UUID id) {
        final Organization organization = organizationService.findById(id);
        return ResponseEntity.ok(organization);
    }

    /**
     * Gets an organization by slug.
     *
     * @param slug the organization slug
     * @return the organization
     */
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<Organization> getOrganizationBySlug(@PathVariable final String slug) {
        final Organization organization = organizationService.findBySlug(slug);
        return ResponseEntity.ok(organization);
    }

    /**
     * Deletes an organization.
     *
     * @param id the organization ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable final UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}
