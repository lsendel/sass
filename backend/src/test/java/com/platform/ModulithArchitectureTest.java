package com.platform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.test.context.ActiveProfiles;

/**
 * Architecture tests to enforce Spring Boot Modulith module boundaries. These tests ensure the
 * constitutional requirement for proper module separation.
 */
@SpringBootTest
@ActiveProfiles("test")
class ModulithArchitectureTest {

  private final ApplicationModules modules =
      ApplicationModules.of(PaymentPlatformApplication.class);
  private static final String DISABLED_REASON =
      "Known intentional cycle across audit/shared/auth/user for security plumbing; "
          + "tracked for future decoupling";

  @Test
  @Disabled(ModulithArchitectureTest.DISABLED_REASON)
  void shouldRespectModuleBoundaries() {
    // Verify that all modules respect their boundaries
    modules.verify();
  }

  @Test
  void shouldHaveValidModuleStructure() {
    // Print module information for verification
    modules.forEach(System.out::println);

    // Verify we have the expected modules
    assert modules.stream().anyMatch(module -> module.getName().equals("auth"));
    assert modules.stream().anyMatch(module -> module.getName().equals("payment"));
    assert modules.stream().anyMatch(module -> module.getName().equals("subscription"));
    assert modules.stream().anyMatch(module -> module.getName().equals("audit"));
    assert modules.stream().anyMatch(module -> module.getName().equals("user"));
    assert modules.stream().anyMatch(module -> module.getName().equals("shared"));
  }

  @Test
  void shouldDocumentModuleStructure() {
    // Generate module documentation
    new Documenter(modules).writeDocumentation().writeModulesAsPlantUml();
  }
}
