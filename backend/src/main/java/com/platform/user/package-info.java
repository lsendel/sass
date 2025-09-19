/**
 * User and organization management module with multi-tenancy support.
 *
 * <p>This module handles user management, organization management,
 * user invitations, role-based access control, and multi-tenant data isolation.
 *
 * <p>Module exports only API interfaces and events to other modules,
 * keeping all implementation details in the internal package.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.user.api} - Public interfaces for user services</li>
 *   <li>{@code com.platform.user.events} - User and organization domain events</li>
 * </ul>
 *
 * <h3>Module Dependencies:</h3>
 * <ul>
 *   <li>{@code com.platform.shared} - Common types and utilities</li>
 *   <li>{@code com.platform.audit} - Event publishing for compliance</li>
 * </ul>
 *
 * @author Payment Platform Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationModule(
    displayName = "User and Organization Management Module",
    allowedDependencies = {"shared", "audit"}
)
package com.platform.user;

import org.springframework.modulith.ApplicationModule;