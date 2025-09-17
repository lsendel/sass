package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@SpringBootApplication
@Modulith(sharedModules = "shared")
public final class PaymentPlatformApplication {

  private PaymentPlatformApplication() {
    // no-op: prevent instantiation
  }

  public static void main(final String[] args) {
    SpringApplication.run(PaymentPlatformApplication.class, args);
  }
}
