package com.platform.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.modulith.test.AssertableApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests for Spring Modulith module boundaries and architecture validation.
 * These tests enforce the constitutional requirements for module isolation and dependency rules.
 *
 * CRITICAL: These tests MUST FAIL initially as no module structure exists yet.
 * This follows the TDD RED-GREEN-Refactor cycle.
 */
@AnalyzeClasses(packages = "com.platform", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Modulith Architecture Tests")
class ModulithArchitectureTest {

    /**
     * Verify that Spring Modulith module structure is valid
     */
    @Test
    @DisplayName("Should have valid Spring Modulith module structure")
    void shouldHaveValidModulithStructure() {
        var modules = AssertableApplicationModules.of("com.platform");

        // Verify all expected modules exist
        modules.verify()
            .containsModules("auth", "user", "payment", "subscription", "audit", "shared");
    }

    /**
     * Verify module dependencies follow the defined architecture
     */
    @Test
    @DisplayName("Should enforce module dependency rules")
    void shouldEnforceModuleDependencyRules() {
        var modules = AssertableApplicationModules.of("com.platform");

        // Shared module should have no dependencies on other modules
        modules.verify()
            .module("shared")
            .doesNotDependOn("auth", "user", "payment", "subscription", "audit");
    }

    /**
     * Auth module architectural rules
     */
    @ArchTest
    @DisplayName("Auth module should only access shared module")
    static final ArchRule authModuleDependencies =
        classes()
            .that().resideInAPackage("..auth..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "..auth..",
                "..shared..",
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta.."
            );

    /**
     * User module architectural rules
     */
    @ArchTest
    @DisplayName("User module should only access shared and auth modules")
    static final ArchRule userModuleDependencies =
        classes()
            .that().resideInAPackage("..user..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "..user..",
                "..shared..",
                "..auth.api..",  // Can access auth API only
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta.."
            );

    /**
     * Payment module architectural rules
     */
    @ArchTest
    @DisplayName("Payment module should only access shared, auth, and user API")
    static final ArchRule paymentModuleDependencies =
        classes()
            .that().resideInAPackage("..payment..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "..payment..",
                "..shared..",
                "..auth.api..",
                "..user.api..",  // Can access user API only
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta..",
                "com.stripe.."  // Stripe integration allowed
            );

    /**
     * Subscription module architectural rules
     */
    @ArchTest
    @DisplayName("Subscription module should only access shared, auth, user, and payment APIs")
    static final ArchRule subscriptionModuleDependencies =
        classes()
            .that().resideInAPackage("..subscription..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "..subscription..",
                "..shared..",
                "..auth.api..",
                "..user.api..",
                "..payment.api..",  // Can access payment API only
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta.."
            );

    /**
     * Audit module architectural rules
     */
    @ArchTest
    @DisplayName("Audit module should only access shared module")
    static final ArchRule auditModuleDependencies =
        classes()
            .that().resideInAPackage("..audit..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "..audit..",
                "..shared..",
                "java..",
                "org.springframework..",
                "org.slf4j..",
                "jakarta.."
            );

    /**
     * Internal packages should not be accessed from outside their module
     */
    @ArchTest
    @DisplayName("Internal packages should not be accessed externally")
    static final ArchRule internalPackagesShouldBePrivate =
        noClasses()
            .that().resideOutsideOfPackages("..auth..")
            .should().accessClassesThat()
            .resideInAPackage("..auth.internal..")
            .orShould().accessClassesThat()
            .resideInAPackage("..user.internal..")
            .orShould().accessClassesThat()
            .resideInAPackage("..payment.internal..")
            .orShould().accessClassesThat()
            .resideInAPackage("..subscription.internal..")
            .orShould().accessClassesThat()
            .resideInAPackage("..audit.internal..");

    /**
     * API packages should only contain DTOs, controllers, and interfaces
     */
    @ArchTest
    @DisplayName("API packages should only contain controllers and DTOs")
    static final ArchRule apiPackagesShouldOnlyContainControllers =
        classes()
            .that().resideInAPackage("..api..")
            .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
            .orShould().beRecords()  // DTOs as records
            .orShould().beInterfaces()
            .orShould().beEnums();

    /**
     * Service classes should be in internal packages
     */
    @ArchTest
    @DisplayName("Service classes should be in internal packages")
    static final ArchRule servicesShouldBeInternal =
        classes()
            .that().areAnnotatedWith("org.springframework.stereotype.Service")
            .should().resideInAPackage("..internal..");

    /**
     * Repository classes should be in internal packages
     */
    @ArchTest
    @DisplayName("Repository classes should be in internal packages")
    static final ArchRule repositoriesShouldBeInternal =
        classes()
            .that().areAnnotatedWith("org.springframework.stereotype.Repository")
            .or().areAssignableTo("org.springframework.data.repository.Repository")
            .should().resideInAPackage("..internal..");

    /**
     * Entity classes should be in internal packages
     */
    @ArchTest
    @DisplayName("Entity classes should be in internal packages")
    static final ArchRule entitiesShouldBeInternal =
        classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..internal..");

    /**
     * Configuration classes should be properly placed
     */
    @ArchTest
    @DisplayName("Configuration classes should be in config packages")
    static final ArchRule configurationClassesPlacement =
        classes()
            .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
            .should().resideInAPackage("..config..")
            .orShould().resideInAPackage("..shared.config..");

    /**
     * No cycles between modules
     */
    @ArchTest
    @DisplayName("Modules should not have circular dependencies")
    static final ArchRule modulesShoudNotHaveCycles =
        slices()
            .matching("com.platform.(*)..")
            .should().beFreeOfCycles();

    /**
     * Layered architecture within modules
     */
    @ArchTest
    @DisplayName("Should follow layered architecture within modules")
    static final ArchRule layeredArchitectureWithinModules =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("..api..")
            .layer("Service").definedBy("..internal..")
            .layer("Repository").definedBy("..internal..")
            .layer("Entity").definedBy("..internal..")

            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service", "Repository");

    /**
     * Event classes should follow naming convention
     */
    @ArchTest
    @DisplayName("Event classes should follow naming convention")
    static final ArchRule eventClassesNamingConvention =
        classes()
            .that().resideInAPackage("..events..")
            .should().haveSimpleNameEndingWith("Event")
            .andShould().beRecords();

    /**
     * Test that all modules can be independently verified
     */
    @Test
    @DisplayName("All modules should be independently verifiable")
    void allModulesShouldBeVerifiable() {
        var modules = AssertableApplicationModules.of("com.platform");

        // Each module should be self-contained and verifiable
        modules.forEach(module -> {
            module.verify();
        });
    }

    /**
     * Security: sensitive annotations should only be in auth module
     */
    @ArchTest
    @DisplayName("Security annotations should only be in auth module")
    static final ArchRule securityAnnotationsShouldBeInAuthModule =
        classes()
            .that().areAnnotatedWith("org.springframework.security.access.prepost.PreAuthorize")
            .should().resideInAPackage("..auth..")
            .orShould().resideInAPackage("..shared.security..");

    /**
     * Payment-related annotations should only be in payment module
     */
    @ArchTest
    @DisplayName("Stripe-related code should only be in payment module")
    static final ArchRule stripeCodeShouldBeInPaymentModule =
        classes()
            .that().dependOnClassesThat().resideInAPackage("com.stripe..")
            .should().resideInAPackage("..payment..");
}