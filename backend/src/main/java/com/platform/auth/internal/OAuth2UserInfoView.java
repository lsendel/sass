package com.platform.auth.internal;

/** Read-only projection of OAuth2UserInfo for API layer. */
public record OAuth2UserInfoView(
    String providerUniqueId,
    String providerUserId,
    String email,
    String displayName,
    String picture,
    boolean emailVerified) {

  public static OAuth2UserInfoView fromEntity(OAuth2UserInfo userInfo) {
    return new OAuth2UserInfoView(
        userInfo.getProviderUniqueId(),
        userInfo.getProviderUserId(),
        userInfo.getEmail(),
        userInfo.getDisplayName(),
        userInfo.getPicture(),
        userInfo.isEmailVerified());
  }
}

