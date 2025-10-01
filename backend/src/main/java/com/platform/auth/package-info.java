/**
 * Authentication and authorization module.
 *
 * <p>This module handles:
 * <ul>
 *   <li>User authentication via OAuth2/OIDC</li>
 *   <li>Opaque token management with Redis</li>
 *   <li>Session management</li>
 *   <li>Password authentication (when enabled)</li>
 *   <li>Role-based access control integration</li>
 * </ul>
 *
 * <p><strong>Module Boundaries:</strong>
 * <ul>
 *   <li>Public API: {@code api} package</li>
 *   <li>Events: {@code events} package</li>
 *   <li>Internal implementation: {@code internal} package (not accessible to other modules)</li>
 * </ul>
 *
 * <p><strong>Dependencies:</strong>
 * <ul>
 *   <li>shared (for common events and exceptions)</li>
 *   <li>audit (publishes authentication events)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Authentication & Authorization",
    allowedDependencies = {"shared", "audit"}
)
package com.platform.auth;
