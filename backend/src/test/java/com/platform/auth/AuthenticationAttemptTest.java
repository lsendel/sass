package com.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.platform.auth.internal.AuthenticationAttempt;

class AuthenticationAttemptTest {

  @Test
  void successFactoryShouldPopulateFields() {
    UUID userId = UUID.randomUUID();
    AuthenticationAttempt attempt =
        AuthenticationAttempt.success(
            userId,
            "user@example.com",
            AuthenticationAttempt.AuthenticationMethod.PASSWORD,
            "10.0.0.1",
            "JUnit",
            "session-1");

    assertThat(attempt.getUserId()).isEqualTo(userId);
    assertThat(attempt.isSuccess()).isTrue();
    assertThat(attempt.getFailureReason()).isNull();
    assertThat(attempt.getSessionId()).isEqualTo("session-1");
  }

  @Test
  void failureFactoryShouldAllowUnknownUsers() {
    AuthenticationAttempt attempt =
        AuthenticationAttempt.failureUnknownUser(
            "ghost@example.com",
            AuthenticationAttempt.AuthenticationMethod.PASSWORD,
            "USER_NOT_FOUND",
            "10.0.0.2",
            "JUnit");

    assertThat(attempt.getUserId()).isNull();
    assertThat(attempt.isFailure()).isTrue();
    assertThat(attempt.getFailureReason()).isEqualTo("USER_NOT_FOUND");
  }
}
