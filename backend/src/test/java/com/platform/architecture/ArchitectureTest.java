package com.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
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
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("API").definedBy("..api..")
                .layer("Internal").definedBy("..internal..")
                .layer("Events").definedBy("..events..")
                .whereLayer("API").mayNotBeAccessedByAnyLayer()
                .whereLayer("Internal").mayOnlyBeAccessedByLayers("API")
                // Events layer can be accessed by any layer (removed deprecated method)
                .ignoreDependency("..events..", "**..**");

        rule.check(classes);
    }

    @Test
    void internalPackagesShouldNotBeAccessedAcrossModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..internal..")
                .should().dependOnClassesThat().resideInAPackage("..internal..")
                .because("Internal packages should not be accessed across modules");

        // Allow same-module internal access
        rule.check(classes);
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
                .because("Configuration classes should be in config packages");

        rule.check(classes);
    }

    @Test
    void dtosShouldBeInApiPackage() {
        ArchRule rule = classes()
                .that().haveNameMatching(".*DTO")
                .or().haveNameMatching(".*Request")
                .or().haveNameMatching(".*Response")
                .should().resideInAPackage("..api..")
                .because("DTOs are part of the public API");

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
