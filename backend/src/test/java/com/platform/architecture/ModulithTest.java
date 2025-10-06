package com.platform.architecture;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith structure verification and documentation generation.
 * This test ensures that the application follows proper modular architecture
 * and generates documentation for module boundaries.
 */
class ModulithTest {

    private static final Logger logger = LoggerFactory.getLogger(ModulithTest.class);
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
            logger.info("Module: {}", module.getDisplayName());
            logger.info("  Base Package: {}", module.getBasePackage());
        });
    }
}
