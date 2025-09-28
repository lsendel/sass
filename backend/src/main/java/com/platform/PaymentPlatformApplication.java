package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * The main entry point for the Payment Platform application.
 *
 * <p>This class is responsible for bootstrapping and launching the Spring Boot application.
 * It is annotated with {@link SpringBootApplication} to enable auto-configuration, component scanning,
 * and other Spring Boot features. The {@link Modulith} annotation configures the application's
 * modular structure, specifying shared modules.
 * </p>
 *
 * @see SpringBootApplication
 * @see Modulith
 */
@SpringBootApplication
@Modulith(sharedModules = "shared")
public final class PaymentPlatformApplication {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PaymentPlatformApplication() {
        // no-op: prevent instantiation
    }

    /**
     * The main method that serves as the entry point for the Java application.
     *
     * <p>This method uses {@link SpringApplication#run(Class, String...)} to launch the application.
     * </p>
     *
     * @param args The command-line arguments passed to the application.
     */
    public static void main(final String[] args) {
        SpringApplication.run(PaymentPlatformApplication.class, args);
    }
}
