/**
 * Shared utilities and common infrastructure module.
 *
 * <p>This module contains shared utilities, security configurations, common types,
 * cross-cutting concerns, and infrastructure components used across all modules.
 *
 * <p>As a shared module, it should not depend on any other business modules
 * and provides foundational services to the rest of the platform.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.shared.types} - Common value objects and types</li>
 *   <li>{@code com.platform.shared.security} - Security utilities and filters</li>
 *   <li>{@code com.platform.shared.config} - Configuration classes</li>
 * </ul>
 *
 * <h3>Module Dependencies:</h3>
 * <ul>
 *   <li>None - This is a foundational module</li>
 * </ul>
 *
 * @author Payment Platform Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationModule(
    displayName = "Shared Infrastructure Module",
    allowedDependencies = {}
)
package com.platform.shared;

import org.springframework.modulith.ApplicationModule;