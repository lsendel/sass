package com.platform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Separation of Concerns rules for the backend. These focus on keeping Web/API, Service, and
 * Persistence concerns decoupled.
 */
class SeparationOfConcernsTest {

    private JavaClasses classes;

    @BeforeEach
    void setup() {
        classes = new ClassFileImporter().importPackages("com.platform");
    }

    @Test
    void controllersShouldNotAccessRepositoriesDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .and().areNotAnnotatedWith(Profile.class)
                .should().accessClassesThat().haveSimpleNameEndingWith("Repository");
        rule.check(classes);
    }

    @Test
    void controllersShouldNotDependOnInternalEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .and().haveSimpleNameEndingWith("Controller")
                .and().areNotAnnotatedWith(Profile.class)
                .should().accessClassesThat().areAnnotatedWith("jakarta.persistence.Entity");
        rule.check(classes);
    }

    @Test
    void servicesShouldNotDependOnApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..internal..")
                .should().accessClassesThat().resideInAPackage("..api..");
        rule.check(classes);
    }

    @Test
    void repositoriesShouldNotBeUsedByApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .and().areNotAnnotatedWith(Profile.class)
                .should().accessClassesThat().haveSimpleNameEndingWith("Repository");
        rule.check(classes);
    }
}
