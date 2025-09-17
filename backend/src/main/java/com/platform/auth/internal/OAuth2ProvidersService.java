package com.platform.auth.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

/** Service for managing OAuth2 provider configurations and metadata. */
@Service
public class OAuth2ProvidersService {

  private final ClientRegistrationRepository clientRegistrationRepository;

  public OAuth2ProvidersService(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /** Get all configured OAuth2 providers */
  public List<OAuth2ProviderInfo> getAvailableProviders() {
    if (!(clientRegistrationRepository instanceof Iterable)) {
      return List.of();
    }

    Iterable<ClientRegistration> registrations =
        (Iterable<ClientRegistration>) clientRegistrationRepository;

    return StreamSupport.stream(registrations.spliterator(), false)
        .map(this::mapToProviderInfo)
        .toList();
  }

  /** Get a specific OAuth2 provider by registration ID */
  public OAuth2ProviderInfo getProvider(String registrationId) {
    try {
      ClientRegistration registration =
          clientRegistrationRepository.findByRegistrationId(registrationId);

      if (registration == null) {
        return null;
      }

      return mapToProviderInfo(registration);
    } catch (Exception e) {
      return null;
    }
  }

  /** Check if a provider is configured and enabled */
  public boolean isProviderEnabled(String registrationId) {
    return getProvider(registrationId) != null;
  }

  /** Get supported provider names */
  public List<String> getSupportedProviderNames() {
    return getAvailableProviders().stream().map(OAuth2ProviderInfo::name).toList();
  }

  private OAuth2ProviderInfo mapToProviderInfo(ClientRegistration registration) {
    return new OAuth2ProviderInfo(
        registration.getRegistrationId(),
        getProviderDisplayName(registration.getRegistrationId()),
        registration.getProviderDetails().getAuthorizationUri(),
        getProviderIconUrl(registration.getRegistrationId()),
        isProviderSupported(registration.getRegistrationId()));
  }

  private String getProviderDisplayName(String registrationId) {
    return switch (registrationId.toLowerCase()) {
      case "google" -> "Google";
      case "github" -> "GitHub";
      case "microsoft" -> "Microsoft";
      case "linkedin" -> "LinkedIn";
      case "facebook" -> "Facebook";
      default -> registrationId;
    };
  }

  private String getProviderIconUrl(String registrationId) {
    // These would typically be served from your static assets
    return switch (registrationId.toLowerCase()) {
      case "google" -> "/images/providers/google.svg";
      case "github" -> "/images/providers/github.svg";
      case "microsoft" -> "/images/providers/microsoft.svg";
      case "linkedin" -> "/images/providers/linkedin.svg";
      case "facebook" -> "/images/providers/facebook.svg";
      default -> "/images/providers/default.svg";
    };
  }

  private boolean isProviderSupported(String registrationId) {
    // List of officially supported providers
    List<String> supportedProviders = List.of("google", "github", "microsoft");
    return supportedProviders.contains(registrationId.toLowerCase());
  }

  /** OAuth2 provider information DTO */
  public record OAuth2ProviderInfo(
      String name, String displayName, String authorizationUrl, String iconUrl, boolean supported) {

    public Map<String, Object> toMap() {
      return Map.of(
          "name", name,
          "displayName", displayName,
          "authorizationUrl", authorizationUrl,
          "iconUrl", iconUrl,
          "supported", supported);
    }
  }
}
