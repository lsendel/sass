/**
 * User and Organization Management Module.
 *
 * <p>This module handles user profiles, organization management, and multi-tenancy.
 * It is separate from the auth module which handles authentication/authorization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>User profile management (names, preferences, settings)</li>
 *   <li>Organization/tenant management</li>
 *   <li>User-organization relationships</li>
 *   <li>User invitations and onboarding</li>
 *   <li>Organization settings and configuration</li>
 * </ul>
 *
 * <h2>Module Dependencies</h2>
 * <ul>
 *   <li><strong>shared</strong> - Common types and utilities</li>
 *   <li><strong>auth</strong> - User authentication integration</li>
 *   <li><strong>audit</strong> - Event logging for user actions</li>
 * </ul>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.platform.user.api} - Public API for other modules</li>
 *   <li>{@link com.platform.user.events} - Domain events published by this module</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * <ul>
 *   <li>{@link com.platform.user.internal} - Internal implementation (not accessible to other modules)</li>
 * </ul>
 *
 * <h2>Architecture Notes</h2>
 * <ul>
 *   <li>Multi-tenant architecture with organization-level isolation</li>
 *   <li>Integration with auth module for user authentication</li>
 *   <li>Event-driven communication with other modules</li>
 *   <li>GDPR-compliant user data handling</li>
 * </ul>
 *
 * @since 1.0.0
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "User & Organization Management",
    allowedDependencies = {"shared", "auth", "audit"}
)
package com.platform.user;
