package com.platform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests to enforce module boundary rules and architectural constraints. These tests are
 * critical for maintaining proper Spring Boot Modulith architecture.
 */
@ActiveProfiles("test")
class ModuleBoundaryTest {

  private JavaClasses classes;

  @BeforeEach
  void setUp() {
    classes = new ClassFileImporter().importPackages("com.platform");
  }

  @Test
  void authModuleShouldNotAccessOtherModuleInternals() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.platform.auth..")
            .should()
            .accessClassesThat()
            // Allow access to user internals for identity linking and to audit internals for
            // logging
            .resideInAnyPackage(
                "com.platform.payment.internal..", "com.platform.subscription.internal.."
                // audit and user internals are allowed dependencies
                );

    rule.check(classes);
  }

  @Test
  void paymentModuleShouldNotAccessOtherModuleInternals() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.platform.payment..")
            .should()
            .accessClassesThat()
            // Allow audit and user internals for required cross-module ops
            .resideInAnyPackage(
                "com.platform.auth.internal..", "com.platform.subscription.internal.."
                // user and audit internals are allowed
                );

    rule.check(classes);
  }

  @Test
  void subscriptionModuleShouldNotAccessOtherModuleInternals() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.platform.subscription..")
            .should()
            .accessClassesThat()
            // Allow audit internals for logging, payment/user internals for cross-module
            // coordination
            .resideInAnyPackage(
                "com.platform.auth.internal.."
                // payment, user, and audit internals are allowed
                );

    rule.check(classes);
  }

  @Test
  void auditModuleShouldNotAccessOtherModuleInternals() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.platform.audit..")
            .should()
            .accessClassesThat()
            .resideInAnyPackage(
                "com.platform.auth.internal..",
                "com.platform.payment.internal..",
                "com.platform.subscription.internal..",
                "com.platform.user.internal..");

    rule.check(classes);
  }

  @Test
  void userModuleShouldNotAccessOtherModuleInternals() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.platform.user..")
            .should()
            .accessClassesThat()
            // Allow audit internals for logging
            .resideInAnyPackage(
                "com.platform.auth.internal..",
                "com.platform.payment.internal..",
                "com.platform.subscription.internal.."
                // audit internal allowed
                );

    rule.check(classes);
  }

  @Test
  void controllersShouldOnlyBeInApiPackages() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            // Allow internal webhook controller implementation in payment module
            .and()
            .doNotHaveFullyQualifiedName("com.platform.payment.internal.StripeWebhookController")
            // Allow test-profile controllers living under api package
            .and()
            .doNotHaveFullyQualifiedName("com.platform.auth.api.TestAuthFlowController")
            .and()
            .doNotHaveFullyQualifiedName("com.platform.payment.api.TestPaymentController")
            .and()
            .doNotHaveFullyQualifiedName("com.platform.subscription.api.TestSubscriptionController")
            .should()
            .resideOutsideOfPackages("..api..");

    rule.check(classes);
  }

  @Test
  void servicesShouldOnlyBeInInternalPackages() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Service")
            .and()
            .areNotInterfaces()
            // Allow shared CustomOAuth2UserService in security package
            .and()
            .doNotHaveFullyQualifiedName("com.platform.shared.security.CustomOAuth2UserService")
            .should()
            .resideOutsideOfPackages("..internal..");

    rule.check(classes);
  }

  @Test
  void repositoriesShouldOnlyBeInInternalPackages() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .resideOutsideOfPackages("..internal..");

    rule.check(classes);
  }

  @Test
  void entitiesShouldOnlyBeInInternalPackages() {
    ArchRule rule =
        noClasses()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideOutsideOfPackages("..internal..");

    rule.check(classes);
  }

  @Test
  void eventsShouldOnlyBeInEventsPackages() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Event")
            // Allow domain event aggregates stored in internal modules
            .and()
            .doNotHaveFullyQualifiedName("com.platform.audit.internal.AuditEvent")
            .and()
            .doNotHaveFullyQualifiedName("com.platform.auth.internal.OAuth2AuditEvent")
            .should()
            .resideOutsideOfPackages("..events..");

    rule.check(classes);
  }
}
