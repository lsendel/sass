/**
 * Shared module containing common utilities, events, and types used across all modules.
 *
 * <p>This module provides:
 * <ul>
 *   <li>Common domain events for inter-module communication</li>
 *   <li>Shared value objects (Money, Email, etc.)</li>
 *   <li>Common exceptions and error handling</li>
 *   <li>Security utilities</li>
 * </ul>
 *
 * <p><strong>Module Boundaries:</strong>
 * All packages in this module are public and accessible to other modules.
 *
 * @since 1.0.0
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared Utilities",
    allowedDependencies = {}
)
package com.platform.shared;
