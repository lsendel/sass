package com.platform.shared.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect to enforce tenant isolation across all data access operations.
 */
@Aspect
@Component
public class TenantIsolationAspect {

    @Around("@annotation(tenantIsolated)")
    public Object enforceTenantIsolation(ProceedingJoinPoint joinPoint, TenantIsolated tenantIsolated) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !(auth.getPrincipal() instanceof PlatformUserPrincipal)) {
            throw new AccessDeniedException("Authentication required for tenant-isolated operation");
        }

        PlatformUserPrincipal principal = (PlatformUserPrincipal) auth.getPrincipal();
        String currentTenant = principal.getOrganizationId();
        
        if (currentTenant == null) {
            throw new AccessDeniedException("Tenant context required");
        }

        // Set tenant context for the operation
        TenantContext.setCurrentTenant(currentTenant);
        
        try {
            return joinPoint.proceed();
        } finally {
            TenantContext.clear();
        }
    }
}
