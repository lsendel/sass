package com.platform.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationMember;
import com.platform.user.internal.OrganizationMemberRepository;
import org.springframework.test.util.ReflectionTestUtils;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationMember;
import com.platform.user.internal.OrganizationMemberRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserService;

class OpaqueTokenStoreTest {

  private TokenMetadataRepository tokenMetadataRepository;
  private UserService userService;
  private PasswordEncoder passwordEncoder;
  private OrganizationMemberRepository memberRepository;

  private OpaqueTokenStore tokenStore;

  @BeforeEach
  void setUp() {
    tokenMetadataRepository = mock(TokenMetadataRepository.class);
    userService = mock(UserService.class);
    passwordEncoder = new BCryptPasswordEncoder();
    memberRepository = mock(OrganizationMemberRepository.class);
    tokenStore =
        new OpaqueTokenStore(
            tokenMetadataRepository, passwordEncoder, userService, memberRepository);
  }

  @Test
  void validateTokenShouldReturnPrincipalWhenLookupHashMatches() {
    String token = "test-token";
    String salt = base64("static-salt");
    String tokenHash = passwordEncoder.encode(token + salt);
    Instant expires = Instant.now().plusSeconds(3600);
    UUID userId = UUID.randomUUID();

    TokenMetadata metadata =
        TokenMetadata.createApiToken(userId, tokenHash, salt, expires, "api-key");
    metadata.setTokenLookupHash(hash(token));

    when(tokenMetadataRepository.findFirstByTokenLookupHashAndExpiresAtAfter(any(), any()))
        .thenReturn(Optional.of(metadata));
    UUID organizationId = UUID.randomUUID();
    Organization organization = new Organization("Test Org", "test-org", UUID.randomUUID());
    ReflectionTestUtils.setField(organization, "id", organizationId);
    User user = new User("user@test.dev", "Test");
    ReflectionTestUtils.setField(user, "id", userId);
    user.setOrganization(organization);

    when(userService.findById(userId)).thenReturn(Optional.of(user));
    when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
        .thenReturn(Optional.of(OrganizationMember.createAdmin(userId, organizationId)));
    when(tokenMetadataRepository.save(metadata)).thenReturn(metadata);

    Optional<PlatformUserPrincipal> principal = tokenStore.validateToken(token);

    assertThat(principal).isPresent();
    PlatformUserPrincipal resolved = principal.orElseThrow();
    assertThat(resolved.getUserId()).isEqualTo(userId);
    assertThat(resolved.getOrganizationSlug()).isEqualTo("test-org");
    assertThat(resolved.getRole()).isEqualTo("ADMIN");
    verify(tokenMetadataRepository).save(metadata);
    verify(tokenMetadataRepository, never()).findValidTokens(any());
  }

  @Test
  void validateTokenBackfillsLookupHashWhenMissing() {
    String token = "legacy-token";
    String salt = base64("legacy-salt");
    String tokenHash = passwordEncoder.encode(token + salt);
    Instant expires = Instant.now().plusSeconds(3600);
    UUID userId = UUID.randomUUID();

    TokenMetadata metadata =
        TokenMetadata.createOAuthSession(userId, tokenHash, salt, expires, "google", "127.0.0.1");

    when(tokenMetadataRepository.findFirstByTokenLookupHashAndExpiresAtAfter(any(), any()))
        .thenReturn(Optional.empty());
    when(tokenMetadataRepository.findValidTokens(any()))
        .thenReturn(List.of(metadata));
    UUID organizationId = UUID.randomUUID();
    Organization organization = new Organization("Legacy Org", "legacy-org", UUID.randomUUID());
    ReflectionTestUtils.setField(organization, "id", organizationId);
    User user = new User("legacy@test.dev", "Legacy");
    ReflectionTestUtils.setField(user, "id", userId);
    user.setOrganization(organization);

    when(userService.findById(userId)).thenReturn(Optional.of(user));
    when(memberRepository.findByOrganizationIdAndUserId(organizationId, userId))
        .thenReturn(Optional.of(OrganizationMember.createMember(userId, organizationId)));
    when(tokenMetadataRepository.save(metadata)).thenReturn(metadata);

    Optional<PlatformUserPrincipal> principal = tokenStore.validateToken(token);

    assertThat(principal).isPresent();
    PlatformUserPrincipal resolved = principal.orElseThrow();
    assertThat(resolved.getRole()).isEqualTo("MEMBER");
    assertThat(metadata.getTokenLookupHash()).isEqualTo(hash(token));
    verify(tokenMetadataRepository, atLeast(2)).save(metadata);
  }

  private static String hash(String token) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(sha256(token.getBytes(StandardCharsets.UTF_8)));
  }

  private static byte[] sha256(byte[] bytes) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      return digest.digest(bytes);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String base64(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
