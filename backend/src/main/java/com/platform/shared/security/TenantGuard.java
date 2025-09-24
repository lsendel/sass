package com.platform.shared.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper component exposed to Spring Security SpEL expressions for tenant-aware authorization
 * checks.
 */
@Component("tenantGuard")
public class TenantGuard {

  public boolean canAccessOrganization(UUID organizationId) {
    return canAccessOrganization(resolvePrincipal(), organizationId);
  }

  public boolean canAccessOrganization(PlatformUserPrincipal principal, UUID organizationId) {
    if (principal == null || organizationId == null) {
      return false;
    }
    return principal.belongsToOrganization(organizationId);
  }

  public boolean canManageOrganization(UUID organizationId) {
    return canManageOrganization(resolvePrincipal(), organizationId);
  }

  public boolean canManageOrganization(PlatformUserPrincipal principal, UUID organizationId) {
    if (principal == null || organizationId == null) {
      return false;
    }
    return principal.isAdmin() && principal.belongsToOrganization(organizationId);
  }

  public boolean isAdmin(PlatformUserPrincipal principal) {
    return principal != null && principal.isAdmin();
  }

  private PlatformUserPrincipal resolvePrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof PlatformUserPrincipal platformUserPrincipal) {
      return platformUserPrincipal;
    }
    return null;
  }
}
