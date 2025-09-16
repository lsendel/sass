package com.platform.user.events;

import com.platform.user.internal.OrganizationMember;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an organization invitation is created.
 * This event can be consumed by email notification services to send
 * invitation emails to invited users.
 */
public record InvitationCreatedEvent(
    UUID invitationId,
    String email,
    UUID organizationId,
    String organizationName,
    OrganizationMember.Role role,
    UUID invitedBy,
    Instant createdAt
) {
    public InvitationCreatedEvent(UUID invitationId, String email, UUID organizationId,
                                String organizationName, OrganizationMember.Role role, UUID invitedBy) {
        this(invitationId, email, organizationId, organizationName, role, invitedBy, Instant.now());
    }
}