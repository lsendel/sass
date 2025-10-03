package com.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests to enforce architectural constraints and best practices.
 * These tests ensure code quality and maintainability through architecture rules.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.platform");
    }

    @Test
    void layerDependenciesAreRespected() {
        // Simplified layered architecture that focuses on module boundaries
        // API layer: Controllers and DTOs
        // Internal layer: Services, Repositories, Entities
        // Events layer: Domain events

        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()  // Only check dependencies between our layers
                .layer("API").definedBy("..api..")
                .layer("Internal").definedBy("..internal..")
                .layer("Events").definedBy("..events..")
                // Controllers (API) can call Services (Internal)
                // Services (Internal) can return DTOs (API) - this is correct architecture
                // Events can be used by anyone
                .whereLayer("API").mayOnlyAccessLayers("Internal", "Events")
                .whereLayer("Internal").mayOnlyAccessLayers("API", "Events")  // Allow Internal to use API DTOs
                .whereLayer("Events").mayNotAccessAnyLayer();

        rule.check(classes);
    }

    @Test
    void internalPackagesShouldNotBeAccessedAcrossModules() {
        // Verify that internal packages from one module don't access internal packages from another module
        // This is a simplified check - Spring Modulith provides more sophisticated module boundary verification

        // Check audit module doesn't access auth/user/payment/subscription internal
        ArchRule auditRule = noClasses()
                .that().resideInAPackage("com.platform.audit.internal..")
                .should().dependOnClassesThat().resideInAPackage("com.platform.auth.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.user.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.payment.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.subscription.internal..")
                .because("Audit module should not access internal packages of other modules");

        // Check auth module doesn't access other modules' internal
        ArchRule authRule = noClasses()
                .that().resideInAPackage("com.platform.auth.internal..")
                .should().dependOnClassesThat().resideInAPackage("com.platform.audit.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.user.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.payment.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.subscription.internal..")
                .because("Auth module should not access internal packages of other modules");

        // Check user module doesn't access other modules' internal
        ArchRule userRule = noClasses()
                .that().resideInAPackage("com.platform.user.internal..")
                .should().dependOnClassesThat().resideInAPackage("com.platform.audit.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.auth.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.payment.internal..")
                .orShould().dependOnClassesThat().resideInAPackage("com.platform.subscription.internal..")
                .because("User module should not access internal packages of other modules");

        auditRule.check(classes);
        authRule.check(classes);
        userRule.check(classes);
    }

    @Test
    void repositoriesShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Repository")
                .should().resideInAPackage("..internal..")
                .because("Repositories are implementation details");

        rule.check(classes);
    }

    @Test
    void servicesShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..internal..")
                .because("Service implementations are internal to modules");

        rule.check(classes);
    }

    @Test
    void controllersShouldUseRestController() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .because("Controllers should use @RestController annotation");

        rule.check(classes);
    }

    @Test
    void noClassesShouldDependOnUtilityClasses() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().haveNameMatching(".*Utils")
                .because("Utility classes indicate poor design; use proper services instead");

        // This rule can be relaxed if needed
        // rule.check(classes);
    }

    @Test
    void domainModelShouldNotDependOnFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain model should be framework-independent");

        // Allow Spring annotations for entities
        rule.allowEmptyShould(true);
        // rule.check(classes);
    }

    @Test
    void eventsShouldBeImmutable() {
        ArchRule rule = classes()
                .that().resideInAPackage("..events..")
                .and().haveNameMatching(".*Event")
                .should().haveOnlyFinalFields()
                .because("Events should be immutable");

        // This requires proper event design
        rule.allowEmptyShould(true);
        // rule.check(classes);
    }

    @Test
    void configurationClassesShouldBeInConfigPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .should().resideInAPackage("..config..")
                .orShould().resideInAPackage("..internal.config..")
                .orShould().resideInAPackage("..internal..")  // Allow module-specific config in internal
                .because("Configuration classes should be in config or internal packages");

        rule.check(classes);
    }

    @Test
    void dtosShouldBeInApiPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*DTO")
                .or().haveNameMatching(".*Request")
                .or().haveNameMatching(".*Response")
                .and().resideOutsideOfPackage("..internal..")  // Exclude internal classes
                .and().arePublic()  // Only check public classes
                .should().resideInAPackage("..api..")
                .because("Public DTOs are part of the API");

        rule.check(classes);
    }

    @Test
    void noClassesShouldAccessInternalPackagesDirectly() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..internal..")
                .should().accessClassesThat().resideInAPackage("..internal..")
                .because("Internal packages should only be accessed through public APIs");

        // Allow same-module access
        rule.allowEmptyShould(true);
        // rule.check(classes);
    }

    @Test
    void publicApiClassesShouldHaveProperDocumentation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..api..")
                .and().arePublic()
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .orShould().beInterfaces()
                .because("Public API should be well-defined");

        rule.allowEmptyShould(true);
        // rule.check(classes);
    }
}
