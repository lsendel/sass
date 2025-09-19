/**
 * Authentication and authorization module for the payment platform.
 *
 * <p>This module provides OAuth2/PKCE authentication, session management,
 * password authentication, and account security features including lockout
 * protection and audit logging.
 *
 * <p>Module exports only API interfaces and events to other modules,
 * keeping all implementation details in the internal package.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.auth.api} - Public interfaces for authentication services</li>
 *   <li>{@code com.platform.auth.events} - Authentication domain events</li>
 * </ul>
 *
 * <h3>Module Dependencies:</h3>
 * <ul>
 *   <li>{@code com.platform.shared} - Security utilities and configurations</li>
 *   <li>{@code com.platform.audit} - Event publishing for compliance</li>
 * </ul>
 *
 * @author Payment Platform Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationModule(
    displayName = "Authentication Module",
    allowedDependencies = {"shared", "audit"}
)
package com.platform.auth;

import org.springframework.modulith.ApplicationModule;