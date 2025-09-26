package com.platform.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.platform.shared.security.PasswordProperties;
import com.platform.shared.types.Email;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.User;

/** Simple tests to exercise basic entity creation and establish code coverage */
@SpringBootTest
@ActiveProfiles("test")
class SimpleCoverageTest {

  private static final BigDecimal AMOUNT_99_99 = BigDecimal.valueOf(99.99);
  private static final BigDecimal AMOUNT_100_00 = BigDecimal.valueOf(100.00);
  private static final BigDecimal AMOUNT_200_00 = BigDecimal.valueOf(200.00);

  @Test
  @DisplayName("Should create User")
  void testUser() {
    User user = new User("test@example.com", "Test User");
    assertNotNull(user);
    assertEquals("test@example.com", user.getEmail().getValue());
    assertEquals("Test User", user.getName());
  }

  @Test
  @DisplayName("Should create Organization")
  void testOrganization() {
    Organization org = new Organization("Test Org", "test", "Description");
    assertNotNull(org);
    assertEquals("Test Org", org.getName());
    assertEquals("test", org.getSlug());
  }

  @Test
  @DisplayName("Should create Email")
  void testEmail() {
    Email email = new Email("test@example.com");
    assertNotNull(email);
    assertEquals("test@example.com", email.getValue());
  }

  @Test
  @DisplayName("Should create Money")
  void testMoney() {
    Money money = new Money(AMOUNT_99_99, "USD");
    assertNotNull(money);
    assertEquals(AMOUNT_99_99, money.getAmount());
    assertEquals("USD", money.getCurrency());
  }

  @Test
  @DisplayName("Should use PasswordProperties")
  void testPasswordProperties() {
    PasswordProperties props = new PasswordProperties();
    assertNotNull(props);
    // Test what we can access without causing compilation errors
    assertNotNull(props.getPolicy());
    assertNotNull(props.getLockout());
  }

  @Test
  @DisplayName("Should set user properties")
  void testUserProperties() {
    User user = new User("test@example.com", "Test User");
    user.setPasswordHash("hashed_password");
    user.setEmailVerified(true);

    assertEquals("hashed_password", user.getPasswordHash());
    assertTrue(user.getEmailVerified());
  }

  @Test
  @DisplayName("Should handle User-Organization relationship")
  void testUserOrganization() {
    User user = new User("test@example.com", "Test User");
    Organization org = new Organization("Test Org", "test", "Description");

    user.setOrganization(org);
    assertEquals(org, user.getOrganization());
  }

  @Test
  @DisplayName("Should test Money equality")
  void testMoneyEquality() {
    Money money1 = new Money(AMOUNT_100_00, "USD");
    Money money2 = new Money(AMOUNT_100_00, "USD");
    Money money3 = new Money(AMOUNT_200_00, "USD");

    assertEquals(money1, money2);
    assertNotEquals(money1, money3);
  }

  @Test
  @DisplayName("Should test Email equality")
  void testEmailEquality() {
    Email email1 = new Email("test@example.com");
    Email email2 = new Email("test@example.com");
    Email email3 = new Email("other@example.com");

    assertEquals(email1, email2);
    assertNotEquals(email1, email3);
  }
}
