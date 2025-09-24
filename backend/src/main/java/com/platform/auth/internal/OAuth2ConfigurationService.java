package com.platform.auth.internal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

/**
 * Service for OAuth2 configuration operations. Provides API layer access to OAuth2 client
 * registrations while maintaining proper module boundaries.
 */
@Service
public class OAuth2ConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2ConfigurationService.class);
  private static final List<String> SUPPORTED_PROVIDERS = List.of("google", "github");

  private final ClientRegistrationRepository clientRegistrationRepository;

  public OAuth2ConfigurationService(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /**
   * Gets available OAuth2 providers that are properly configured.
   *
   * @return list of available provider configurations
   */
  public List<ProviderInfo> getAvailableProviders() {
    return SUPPORTED_PROVIDERS.stream()
        .map(this::getClientRegistration)
        .filter(Objects::nonNull)
        .map(this::mapToProviderInfo)
        .collect(Collectors.toList());
  }

  /**
   * Checks if a specific OAuth2 provider is configured and available.
   *
   * @param provider the provider name
   * @return true if the provider is available
   */
  public boolean isProviderAvailable(String provider) {
    return getClientRegistration(provider) != null;
  }

  /**
   * Gets client registration for a specific provider.
   *
   * @param provider the provider name
   * @return the client registration if available, null otherwise
   */
  public ClientRegistration getClientRegistration(String provider) {
    try {
      return clientRegistrationRepository.findByRegistrationId(provider);
    } catch (Exception e) {
      logger.debug("Provider {} not configured", provider);
      return null;
    }
  }

  private ProviderInfo mapToProviderInfo(ClientRegistration registration) {
    return new ProviderInfo(
        registration.getRegistrationId(),
        registration.getClientName(),
        registration.getProviderDetails().getAuthorizationUri(),
        registration.getRedirectUri(),
        registration.getScopes());
  }

  /**
   * DTO for OAuth2 provider information to avoid exposing internal details.
   */
  public record ProviderInfo(
      String registrationId,
      String clientName,
      String authorizationUri,
      String redirectUri,
      java.util.Set<String> scopes) {}
}