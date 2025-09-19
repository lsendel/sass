/**
 * Subscription management module for billing cycles and plan management.
 *
 * <p>This module handles subscription lifecycle management, billing cycles,
 * plan configuration, invoice generation, and revenue recognition.
 *
 * <p>Module exports only API interfaces and events to other modules,
 * keeping all implementation details in the internal package.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.subscription.api} - Public interfaces for subscription services</li>
 *   <li>{@code com.platform.subscription.events} - Subscription domain events</li>
 * </ul>
 *
 * <h3>Module Dependencies:</h3>
 * <ul>
 *   <li>{@code com.platform.shared} - Common types and utilities</li>
 *   <li>{@code com.platform.payment} - Payment processing integration</li>
 *   <li>{@code com.platform.audit} - Event publishing for compliance</li>
 * </ul>
 *
 * @author Payment Platform Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationModule(
    displayName = "Subscription Management Module",
    allowedDependencies = {"shared", "payment", "audit"}
)
package com.platform.subscription;

import org.springframework.modulith.ApplicationModule;