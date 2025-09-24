package com.platform.shared.config;

import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.platform.user.internal.DemoUserManagementService;
import com.platform.user.internal.User;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;

/**
 * Demo data loader that creates a demo organization and user for testing purposes. Only runs in the
 * 'test' profile to avoid creating demo data in production.
 */
@Component
@Profile("test")
public class DemoDataLoader implements CommandLineRunner {

  private final DemoUserManagementService demoUserManagementService;
  private final PasswordEncoder passwordEncoder;
  private final OrganizationRepository organizationRepository;

  public DemoDataLoader(
      DemoUserManagementService demoUserManagementService,
      PasswordEncoder passwordEncoder,
      OrganizationRepository organizationRepository) {
    this.demoUserManagementService = demoUserManagementService;
    this.passwordEncoder = passwordEncoder;
    this.organizationRepository = organizationRepository;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    // Check if demo data already exists
    if (!organizationRepository.findAll().isEmpty()) {
      System.out.println("Demo data already exists, skipping creation");
      return;
    }

    System.out.println("Creating demo data for E2E testing...");

    // Create demo organization directly via repository (bypasses security)
    Organization demoOrg = new Organization("Demo Organization", "demo-org", "Demo organization for testing");
    demoOrg = organizationRepository.save(demoOrg);

    System.out.println("Created demo organization: " + demoOrg.getName() + " with ID: " + demoOrg.getId());

    // Create additional demo organization for variety
    Organization demoOrg2 = new Organization("Test Organization", "test-org", "Test organization for E2E testing");
    demoOrg2 = organizationRepository.save(demoOrg2);

    System.out.println("Created test organization: " + demoOrg2.getName() + " with ID: " + demoOrg2.getId());
    System.out.println("Demo data creation completed!");
  }
}
