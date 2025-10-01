package com.platform.user.internal;

import com.platform.shared.exceptions.ResourceNotFoundException;
import com.platform.shared.exceptions.ValidationException;
import com.platform.user.Organization;
import com.platform.user.events.OrganizationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing organizations.
 *
 * @since 1.0.0
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrganizationService(
            final OrganizationRepository organizationRepository,
            final ApplicationEventPublisher eventPublisher) {
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new organization.
     *
     * @param name the organization name
     * @param slug the URL-friendly slug
     * @return the created organization
     * @throws ValidationException if slug already exists
     */
    @Transactional
    public Organization createOrganization(final String name, final String slug) {
        if (organizationRepository.existsBySlug(slug)) {
            throw new ValidationException("Organization slug already exists: " + slug);
        }

        final Organization organization = new Organization(name, slug);
        final Organization saved = organizationRepository.save(organization);

        // Publish event for other modules
        eventPublisher.publishEvent(new OrganizationCreatedEvent(
            saved.getId(),
            saved.getName(),
            saved.getSlug(),
            saved.getCreatedAt()
        ));

        return saved;
    }

    /**
     * Finds an organization by ID.
     *
     * @param id the organization ID
     * @return the organization
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Organization findById(final UUID id) {
        return organizationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }

    /**
     * Finds an organization by slug.
     *
     * @param slug the organization slug
     * @return the organization
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Organization findBySlug(final String slug) {
        return organizationRepository.findActiveBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + slug));
    }

    /**
     * Updates an organization.
     *
     * @param id the organization ID
     * @param name the new name
     * @return the updated organization
     */
    @Transactional
    public Organization updateOrganization(final UUID id, final String name) {
        final Organization organization = findById(id);
        organization.setName(name);
        return organizationRepository.save(organization);
    }

    /**
     * Suspends an organization.
     *
     * @param id the organization ID
     */
    @Transactional
    public void suspendOrganization(final UUID id) {
        final Organization organization = findById(id);
        organization.suspend();
        organizationRepository.save(organization);
    }

    /**
     * Deletes an organization (soft delete).
     *
     * @param id the organization ID
     */
    @Transactional
    public void deleteOrganization(final UUID id) {
        final Organization organization = findById(id);
        organization.delete();
        organizationRepository.save(organization);
    }
}
