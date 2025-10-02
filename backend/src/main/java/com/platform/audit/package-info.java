/**
 * Audit Logging Module.
 *
 * <p>This module provides comprehensive audit logging and compliance tracking
 * for all platform activities, supporting GDPR, SOC2, and PCI DSS requirements.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Comprehensive audit event logging</li>
 *   <li>Audit log search and filtering</li>
 *   <li>Audit log export (CSV, JSON, PDF)</li>
 *   <li>Compliance reporting and analytics</li>
 *   <li>Security event tracking</li>
 *   <li>Forensic analysis support</li>
 * </ul>
 *
 * <h2>Module Dependencies</h2>
 * <ul>
 *   <li><strong>shared</strong> - Common types, exceptions, and security utilities</li>
 * </ul>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.platform.audit.api} - Public API for audit log viewing and export</li>
 *   <li>{@link com.platform.audit.domain} - Domain events for audit tracking</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * <ul>
 *   <li>{@link com.platform.audit.internal} - Internal implementation (not accessible to other modules)</li>
 * </ul>
 *
 * <h2>Architecture Notes</h2>
 * <ul>
 *   <li>Event-driven architecture for real-time audit logging</li>
 *   <li>Immutable audit events with cryptographic integrity</li>
 *   <li>Retention policies for GDPR compliance</li>
 *   <li>High-performance search with PostgreSQL full-text search</li>
 *   <li>Role-based access control for audit log access</li>
 * </ul>
 *
 * @since 1.0.0
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Audit Logging",
    allowedDependencies = {"shared"}
)
package com.platform.audit;
