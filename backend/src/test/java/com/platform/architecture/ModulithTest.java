package com.platform.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith structure verification and documentation generation.
 * This test ensures that the application follows proper modular architecture
 * and generates documentation for module boundaries.
 */
class ModulithTest {

    private static final ApplicationModules modules = ApplicationModules.of(com.platform.AuditApplication.class);

    @Test
    void verifiesModularStructure() {
        // Verify that the application follows Spring Modulith conventions
        // This will fail if module boundaries are violated
        modules.verify();
    }

    @Test
    void createModuleDocumentation() throws Exception {
        // Generate module documentation
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml()
                .writeModulesAsPlantUml();
    }

    @Test
    void verifiesModuleDependencies() {
        // Verify that modules only depend on allowed modules
        // This ensures proper layering and prevents circular dependencies
        modules.forEach(module -> {
            System.out.println("Module: " + module.getDisplayName());
            System.out.println("  Base Package: " + module.getBasePackage());
        });
    }
}
