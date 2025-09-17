package com.platform.shared.security;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** Custom OAuth2 user service for processing OAuth2 user information. */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // Load user from OAuth2 provider
    OAuth2User oauth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    logger.info("Loading OAuth2 user from provider: {}", registrationId);

    // Normalize user attributes based on provider
    OAuth2User normalizedUser = normalizeUserAttributes(oauth2User, registrationId);

    logger.debug(
        "Normalized OAuth2 user: email={}, name={}",
        normalizedUser.getAttribute("email"),
        normalizedUser.getAttribute("name"));

    return normalizedUser;
  }

  private OAuth2User normalizeUserAttributes(OAuth2User oauth2User, String registrationId) {
    var attributes = new java.util.HashMap<>(oauth2User.getAttributes());

    // Normalize attributes based on provider
    switch (registrationId.toLowerCase()) {
      case "google" -> {
        // Google attributes are already in standard format
        // email, name, picture, etc.
      }
      case "github" -> {
        // GitHub uses 'login' for username, 'name' might be null
        if (attributes.get("name") == null) {
          attributes.put("name", attributes.get("login"));
        }
        // GitHub email might be private, get from email endpoint if needed
      }
      case "microsoft" -> {
        // Microsoft uses 'userPrincipalName' for email
        if (attributes.get("email") == null && attributes.get("userPrincipalName") != null) {
          attributes.put("email", attributes.get("userPrincipalName"));
        }
        if (attributes.get("name") == null && attributes.get("displayName") != null) {
          attributes.put("name", attributes.get("displayName"));
        }
      }
    }

    return new CustomOAuth2User(attributes, oauth2User.getAuthorities(), "email");
  }

  /** Custom OAuth2User implementation with normalized attributes */
  private static class CustomOAuth2User implements OAuth2User {
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String nameAttributeKey;

    public CustomOAuth2User(
        Map<String, Object> attributes,
        Collection<? extends GrantedAuthority> authorities,
        String nameAttributeKey) {
      this.attributes = attributes;
      this.authorities = authorities;
      this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
    }

    @Override
    public String getName() {
      return getAttribute(nameAttributeKey);
    }
  }
}
