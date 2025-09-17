package com.platform.shared.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Demo data loader that creates a demo organization and user for testing purposes. Only runs in the
 * 'test' profile to avoid creating demo data in production.
 */
@Component
@Profile("test")
public class DemoDataLoader implements CommandLineRunner {

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public DemoDataLoader(
      OrganizationRepository organizationRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    // Check if demo data already exists by looking for demo user
    if (userRepository.findByEmailAndDeletedAtIsNull("demo@example.com").isPresent()) {
      System.out.println("Demo data already exists, skipping creation");
      return;
    }

    System.out.println("Creating demo data...");

    // Create demo organization (let UUID be auto-generated)
    Organization demoOrg = createDemoOrganization();
    demoOrg = organizationRepository.save(demoOrg);
    System.out.println(
        "Created demo organization: " + demoOrg.getName() + " with ID: " + demoOrg.getId());

    // Create demo user
    User demoUser = createDemoUser(demoOrg);
    demoUser = userRepository.save(demoUser);
    System.out.println("Created demo user: " + demoUser.getEmail().getValue());

    System.out.println("Demo data creation completed!");
    System.out.println("Organization ID: " + demoOrg.getId());
  }

  private Organization createDemoOrganization() {
    return new Organization("Demo Organization", "demo-org", "Demo organization for testing");
  }

  private User createDemoUser(Organization organization) {
    User user = new User("demo@example.com", "Demo User");
    user.setOrganization(organization);
    user.setPasswordHash(passwordEncoder.encode("DemoPassword123!"));
    user.setEmailVerified(true); // Skip email verification for demo
    user.setAuthenticationMethods(Set.of(User.AuthenticationMethod.PASSWORD));

    return user;
  }
}
