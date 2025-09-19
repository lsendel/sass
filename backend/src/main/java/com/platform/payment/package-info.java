/**
 * Payment processing module for Stripe integration and financial transactions.
 *
 * <p>This module handles all payment-related operations including payment processing,
 * payment method management, refunds, and webhook processing with PCI compliance
 * considerations.
 *
 * <p>Module exports only API interfaces and events to other modules,
 * keeping all Stripe integration details in the internal package.
 *
 * <h3>Exported APIs:</h3>
 * <ul>
 *   <li>{@code com.platform.payment.api} - Public interfaces for payment services</li>
 *   <li>{@code com.platform.payment.events} - Payment domain events</li>
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
    displayName = "Payment Processing Module",
    allowedDependencies = {"shared", "audit"}
)
package com.platform.payment;

import org.springframework.modulith.ApplicationModule;