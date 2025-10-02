package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for the Audit Platform.
 * This class is the entry point for the Spring Boot application.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = false)
@EnableCaching
@EnableJpaRepositories(basePackages = "com.platform")
@EntityScan(basePackages = "com.platform")
public final class AuditApplication {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private AuditApplication() {
        // Utility class
    }

    /**
     * Main method to start the application.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}