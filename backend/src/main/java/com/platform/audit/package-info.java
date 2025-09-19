/**
 * Audit and compliance module with GDPR support and forensic capabilities.
 *
 * <p>This module provides comprehensive audit logging, GDPR compliance features,
 * security monitoring, event correlation, and data retention management.
 *
 * <p>This is a foundational module that other modules depend on for compliance
 * and audit trail requirements.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.audit.api} - Public interfaces for audit services</li>
 *   <li>{@code com.platform.audit.events} - Audit domain events</li>
 * </ul>
 *
 * <h3>Module Dependencies:</h3>
 * <ul>
 *   <li>{@code com.platform.shared} - Common types and utilities</li>
 * </ul>
 *
 * @author Payment Platform Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationModule(
    displayName = "Audit and Compliance Module",
    allowedDependencies = {"shared"}
)
package com.platform.audit;

import org.springframework.modulith.ApplicationModule;