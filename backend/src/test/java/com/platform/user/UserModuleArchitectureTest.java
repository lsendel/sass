package com.platform.user;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests for the User module to ensure Spring Modulith compliance.
 * These tests enforce proper module boundaries and clean architecture principles.
 */
class UserModuleArchitectureTest {

    private static final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.platform.user");

    @Test
    @DisplayName("API package should only contain interfaces and records")
    void apiPackageShouldOnlyContainInterfacesAndRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..api..")
            .should().beInterfaces()
            .orShould().beRecords()
            .orShould().beEnums();

        rule.check(classes);
    }

    @Test
    @DisplayName("Internal package should not be accessed from outside the module")
    void internalPackageShouldNotBeAccessedFromOutside() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("com.platform.user..")
            .should().accessClassesThat()
            .resideInAPackage("com.platform.user.internal..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Events package should only contain event classes")
    void eventsPackageShouldOnlyContainEvents() {
        ArchRule rule = classes()
            .that().resideInAPackage("..events..")
            .should().beRecords()
            .orShould().haveSimpleNameEndingWith("Event");

        rule.check(classes);
    }

    @Test
    @DisplayName("Internal services should only be accessed within the module")
    void internalServicesShouldOnlyBeAccessedWithinModule() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("com.platform.user..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.platform.user.internal..")
            .andShould().haveSimpleNameEndingWith("Service");

        rule.check(classes);
    }

    @Test
    @DisplayName("Repositories should only be accessed by services within the module")
    void repositoriesShouldOnlyBeAccessedByServices() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().onlyBeAccessed().byClassesThat()
            .haveSimpleNameEndingWith("Service")
            .orShould().onlyBeAccessed().byClassesThat()
            .haveSimpleNameEndingWith("ApiImpl")
            .orShould().onlyBeAccessed().byClassesThat()
            .areAnnotatedWith("org.springframework.boot.test.context.SpringBootTest");

        rule.check(classes);
    }

    @Test
    @DisplayName("JPA entities should only be accessed within internal package")
    void jpaEntitiesShouldOnlyBeAccessedInternally() {
        ArchRule rule = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAPackage("com.platform.user.internal..")
            .orShould().onlyBeAccessed().byClassesThat()
            .areAnnotatedWith("org.springframework.boot.test.context.SpringBootTest");

        rule.check(classes);
    }

    @Test
    @DisplayName("API implementations should be package-private")
    void apiImplementationsShouldBePackagePrivate() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiImpl")
            .should().bePackagePrivate()
            .andShould().beAnnotatedWith("org.springframework.stereotype.Component");

        rule.check(classes);
    }

    @Test
    @DisplayName("Services should be annotated with @Service")
    void servicesShouldBeAnnotatedWithService() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().resideInAPackage("..internal..")
            .should().beAnnotatedWith("org.springframework.stereotype.Service");

        rule.check(classes);
    }

    @Test
    @DisplayName("Repositories should be annotated with @Repository")
    void repositoriesShouldBeAnnotatedWithRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should().beAnnotatedWith("org.springframework.stereotype.Repository");

        rule.check(classes);
    }

    @Test
    @DisplayName("Services should use constructor injection")
    void servicesShouldUseConstructorInjection() {
        ArchRule rule = noFields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

        rule.check(classes);
    }

    @Test
    @DisplayName("Event classes should be immutable records")
    void eventClassesShouldBeImmutableRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..events..")
            .and().haveSimpleNameEndingWith("Event")
            .should().beRecords();

        rule.check(classes);
    }

    @Test
    @DisplayName("Module should follow layered architecture")
    void moduleShouldFollowLayeredArchitecture() {
        ArchRule rule = layeredArchitecture()
            .layer("API").definedBy("..api..")
            .layer("Events").definedBy("..events..")
            .layer("Services").definedBy("..internal..").and().haveSimpleNameEndingWith("Service")
            .layer("Repositories").definedBy("..internal..").and().haveSimpleNameEndingWith("Repository")
            .layer("Entities").definedBy("..internal..").and().areAnnotatedWith("jakarta.persistence.Entity")

            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Events").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services").mayOnlyBeAccessedByLayers("API")
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
            .whereLayer("Entities").mayOnlyBeAccessedByLayers("Services", "Repositories");

        rule.check(classes);
    }

    @Test
    @DisplayName("No cycles should exist between packages")
    void noPackageCyclesShouldExist() {
        ArchRule rule = slices()
            .matching("com.platform.user.(**)")
            .should().beFreeOfCycles();

        rule.check(classes);
    }
}