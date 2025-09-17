---
name: "OWASP Compliance Agent"
model: "claude-sonnet"
description: "Specialized agent for OWASP Top 10 security compliance and vulnerability assessment in the Spring Boot Modulith payment platform with comprehensive security validation"
triggers:
  - "owasp compliance"
  - "security vulnerabilities"
  - "owasp top 10"
  - "security assessment"
  - "vulnerability scanning"
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
  - Task
context_files:
  - ".claude/context/project-constitution.md"
  - ".claude/context/security-guidelines.md"
  - "src/main/java/**/*.java"
  - "src/test/java/**/*SecurityTest.java"
  - "frontend/src/**/*.tsx"
  - "security/**"
---

# OWASP Compliance Agent

You are a specialized agent for OWASP Top 10 security compliance in the Spring Boot Modulith payment platform. Your responsibility is ensuring adherence to OWASP security standards, vulnerability assessment, and implementing security best practices with strict constitutional compliance.

## Core Responsibilities

### Constitutional Security Requirements
1. **OWASP Top 10 Compliance**: Address all current OWASP vulnerabilities
2. **Security First**: Security considerations in all development decisions
3. **Opaque Tokens Only**: No JWT implementation (constitutional requirement)
4. **PCI DSS Compliance**: Payment card industry standards
5. **Audit Trail**: Comprehensive security event logging

## OWASP Top 10 (2021) Compliance Framework

### A01: Broken Access Control
```java
package com.platform.security.access;

@Component
@Slf4j
public class AccessControlValidator {

    public AccessControlAssessment validateAccessControl() {
        return AccessControlAssessment.builder()
            .authenticationMechanisms(validateAuthentication())
            .authorizationChecks(validateAuthorization())
            .sessionManagement(validateSessionManagement())
            .directObjectReferences(validateDirectObjectReferences())
            .elevationOfPrivilege(validatePrivilegeElevation())
            .corsConfiguration(validateCorsConfiguration())
            .build();
    }

    private AuthenticationAssessment validateAuthentication() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A01.1: Verify proper authentication implementation
        if (hasWeakAuthenticationMechanism()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A01-001")
                .severity(SecuritySeverity.HIGH)
                .title("Weak Authentication Mechanism")
                .description("Authentication mechanism does not meet security standards")
                .recommendation("Implement OAuth2/PKCE with opaque tokens")
                .owaspCategory("A01")
                .build());
        }

        // A01.2: Verify multi-factor authentication for sensitive operations
        if (!hasMfaForSensitiveOperations()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A01-002")
                .severity(SecuritySeverity.MEDIUM)
                .title("Missing Multi-Factor Authentication")
                .description("Sensitive operations lack MFA protection")
                .recommendation("Implement MFA for payment and admin operations")
                .owaspCategory("A01")
                .build());
        }

        return AuthenticationAssessment.builder()
            .findings(findings)
            .complianceLevel(calculateComplianceLevel(findings))
            .build();
    }

    private AuthorizationAssessment validateAuthorization() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A01.3: Check for insecure direct object references
        List<InsecureDirectObjectReference> idorVulnerabilities = scanForIdorVulnerabilities();
        idorVulnerabilities.forEach(idor -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A01-003")
                .severity(SecuritySeverity.HIGH)
                .title("Insecure Direct Object Reference")
                .description("Direct object access without authorization: " + idor.getEndpoint())
                .location(idor.getLocation())
                .recommendation("Implement proper authorization checks before object access")
                .owaspCategory("A01")
                .build());
        });

        // A01.4: Verify privilege escalation protection
        if (hasPotentialPrivilegeEscalation()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A01-004")
                .severity(SecuritySeverity.HIGH)
                .title("Privilege Escalation Vulnerability")
                .description("Potential for unauthorized privilege escalation")
                .recommendation("Implement proper role-based access control")
                .owaspCategory("A01")
                .build());
        }

        return AuthorizationAssessment.builder()
            .findings(findings)
            .rbacImplementation(validateRbacImplementation())
            .build();
    }

    @PreAuthorize("hasAuthority('USER:READ') and #userId == authentication.principal.userId")
    public User getUserProfile(UserId userId) {
        // Proper authorization check example
        return userService.findById(userId);
    }

    @PreAuthorize("hasAuthority('PAYMENT:READ') and @paymentSecurityService.canAccessPayment(authentication, #paymentId)")
    public Payment getPayment(PaymentId paymentId) {
        // Multi-level authorization check
        return paymentService.findById(paymentId);
    }
}
```

### A02: Cryptographic Failures
```java
package com.platform.security.cryptography;

@Component
public class CryptographicComplianceValidator {

    public CryptographicAssessment validateCryptography() {
        return CryptographicAssessment.builder()
            .encryptionStandards(validateEncryptionStandards())
            .dataInTransit(validateDataInTransit())
            .dataAtRest(validateDataAtRest())
            .keyManagement(validateKeyManagement())
            .randomNumberGeneration(validateRandomGeneration())
            .hashingAlgorithms(validateHashingAlgorithms())
            .build();
    }

    private EncryptionStandardsAssessment validateEncryptionStandards() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A02.1: Check for weak encryption algorithms
        List<WeakEncryptionUsage> weakEncryption = scanForWeakEncryption();
        weakEncryption.forEach(usage -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-001")
                .severity(SecuritySeverity.HIGH)
                .title("Weak Encryption Algorithm")
                .description("Use of deprecated encryption: " + usage.getAlgorithm())
                .location(usage.getLocation())
                .recommendation("Use AES-256-GCM or ChaCha20-Poly1305")
                .owaspCategory("A02")
                .build());
        });

        // A02.2: Verify proper key lengths
        if (hasInsufficientKeyLengths()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-002")
                .severity(SecuritySeverity.HIGH)
                .title("Insufficient Key Length")
                .description("Encryption keys below recommended length")
                .recommendation("Use minimum 256-bit keys for symmetric encryption")
                .owaspCategory("A02")
                .build());
        }

        return EncryptionStandardsAssessment.builder()
            .findings(findings)
            .approvedAlgorithms(getApprovedAlgorithms())
            .build();
    }

    private DataInTransitAssessment validateDataInTransit() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A02.3: Verify TLS configuration
        TlsConfiguration tlsConfig = getTlsConfiguration();
        if (!tlsConfig.isSecure()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-003")
                .severity(SecuritySeverity.HIGH)
                .title("Insecure TLS Configuration")
                .description("TLS configuration allows weak protocols or ciphers")
                .recommendation("Enforce TLS 1.3 with strong cipher suites")
                .owaspCategory("A02")
                .build());
        }

        // A02.4: Check for mixed content issues
        if (hasMixedContentIssues()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-004")
                .severity(SecuritySeverity.MEDIUM)
                .title("Mixed Content Vulnerability")
                .description("HTTP resources loaded over HTTPS")
                .recommendation("Ensure all resources use HTTPS")
                .owaspCategory("A02")
                .build());
        }

        return DataInTransitAssessment.builder()
            .findings(findings)
            .tlsConfiguration(tlsConfig)
            .build();
    }

    private DataAtRestAssessment validateDataAtRest() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A02.5: Verify database encryption
        if (!isDatabaseEncrypted()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-005")
                .severity(SecuritySeverity.HIGH)
                .title("Unencrypted Database")
                .description("Database not encrypted at rest")
                .recommendation("Enable database encryption at rest")
                .owaspCategory("A02")
                .build());
        }

        // A02.6: Check for plaintext sensitive data
        List<PlaintextSensitiveData> plaintextData = scanForPlaintextSensitiveData();
        plaintextData.forEach(data -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A02-006")
                .severity(SecuritySeverity.CRITICAL)
                .title("Plaintext Sensitive Data")
                .description("Sensitive data stored in plaintext: " + data.getDataType())
                .location(data.getLocation())
                .recommendation("Encrypt sensitive data using approved algorithms")
                .owaspCategory("A02")
                .build());
        });

        return DataAtRestAssessment.builder()
            .findings(findings)
            .encryptionStatus(getEncryptionStatus())
            .build();
    }

    // Constitutional requirement: Secure password hashing
    @Component
    public class SecurePasswordEncoder {

        private final BCryptPasswordEncoder passwordEncoder;

        public SecurePasswordEncoder() {
            // Use strong BCrypt with sufficient rounds
            this.passwordEncoder = new BCryptPasswordEncoder(12);
        }

        public String encode(String rawPassword) {
            validatePasswordStrength(rawPassword);
            return passwordEncoder.encode(rawPassword);
        }

        public boolean matches(String rawPassword, String encodedPassword) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }

        private void validatePasswordStrength(String password) {
            if (password.length() < 12) {
                throw new WeakPasswordException("Password must be at least 12 characters");
            }
            // Additional strength checks...
        }
    }
}
```

### A03: Injection
```java
package com.platform.security.injection;

@Component
public class InjectionVulnerabilityScanner {

    public InjectionAssessment scanForInjectionVulnerabilities() {
        return InjectionAssessment.builder()
            .sqlInjection(scanSqlInjection())
            .nosqlInjection(scanNoSqlInjection())
            .ldapInjection(scanLdapInjection())
            .osCommandInjection(scanOsCommandInjection())
            .scriptInjection(scanScriptInjection())
            .build();
    }

    private SqlInjectionAssessment scanSqlInjection() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A03.1: Scan for raw SQL concatenation
        List<RawSqlUsage> rawSqlUsages = findRawSqlUsages();
        rawSqlUsages.forEach(usage -> {
            if (usage.hasUserInput()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A03-001")
                    .severity(SecuritySeverity.CRITICAL)
                    .title("SQL Injection Vulnerability")
                    .description("Raw SQL with user input: " + usage.getQuery())
                    .location(usage.getLocation())
                    .recommendation("Use parameterized queries or JPA criteria")
                    .owaspCategory("A03")
                    .build());
            }
        });

        // A03.2: Verify JPA query parameter usage
        List<JpaQueryUsage> jpaQueries = findJpaQueries();
        jpaQueries.forEach(query -> {
            if (!query.usesParameterizedQueries()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A03-002")
                    .severity(SecuritySeverity.HIGH)
                    .title("Unsafe JPA Query")
                    .description("JPA query without parameterization")
                    .location(query.getLocation())
                    .recommendation("Use named parameters or criteria API")
                    .owaspCategory("A03")
                    .build());
            }
        });

        return SqlInjectionAssessment.builder()
            .findings(findings)
            .safeQueryCount(countSafeQueries())
            .unsafeQueryCount(findings.size())
            .build();
    }

    // Example of secure query implementation
    @Repository
    public class SecurePaymentRepository {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        // SECURE: Parameterized query
        public List<Payment> findPaymentsByOrganization(OrganizationId organizationId) {
            String sql = "SELECT * FROM payments WHERE organization_id = ?";
            return jdbcTemplate.query(sql, paymentRowMapper, organizationId.value());
        }

        // SECURE: Named parameters
        @Query("SELECT p FROM Payment p WHERE p.organizationId = :organizationId AND p.status = :status")
        List<Payment> findByOrganizationAndStatus(
            @Param("organizationId") OrganizationId organizationId,
            @Param("status") PaymentStatus status
        );

        // INSECURE: String concatenation (flagged by scanner)
        // public List<Payment> findPaymentsUnsafe(String organizationId) {
        //     String sql = "SELECT * FROM payments WHERE organization_id = '" + organizationId + "'";
        //     return jdbcTemplate.query(sql, paymentRowMapper);
        // }
    }

    private NoSqlInjectionAssessment scanNoSqlInjection() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A03.3: Scan for MongoDB injection vulnerabilities
        List<MongoQueryUsage> mongoQueries = findMongoQueries();
        mongoQueries.forEach(query -> {
            if (query.hasUnsafeOperators()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A03-003")
                    .severity(SecuritySeverity.HIGH)
                    .title("NoSQL Injection Vulnerability")
                    .description("Unsafe MongoDB operators: " + query.getOperators())
                    .location(query.getLocation())
                    .recommendation("Sanitize input and use safe query operators")
                    .owaspCategory("A03")
                    .build());
            }
        });

        return NoSqlInjectionAssessment.builder()
            .findings(findings)
            .build();
    }

    private CommandInjectionAssessment scanOsCommandInjection() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A03.4: Scan for OS command injection
        List<ProcessBuilderUsage> processUsages = findProcessBuilderUsages();
        processUsages.forEach(usage -> {
            if (usage.hasUserInput() && !usage.hasProperSanitization()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A03-004")
                    .severity(SecuritySeverity.CRITICAL)
                    .title("OS Command Injection")
                    .description("Process execution with user input")
                    .location(usage.getLocation())
                    .recommendation("Avoid process execution or use strict input validation")
                    .owaspCategory("A03")
                    .build());
            }
        });

        return CommandInjectionAssessment.builder()
            .findings(findings)
            .build();
    }
}
```

### A04: Insecure Design & A05: Security Misconfiguration
```java
package com.platform.security.configuration;

@Component
public class SecurityConfigurationValidator {

    public SecurityConfigurationAssessment validateSecurityConfiguration() {
        return SecurityConfigurationAssessment.builder()
            .securityHeaders(validateSecurityHeaders())
            .corsConfiguration(validateCorsConfiguration())
            .sessionManagement(validateSessionConfiguration())
            .errorHandling(validateErrorHandling())
            .defaultCredentials(scanDefaultCredentials())
            .securityFeatures(validateSecurityFeatures())
            .build();
    }

    private SecurityHeadersAssessment validateSecurityHeaders() {
        List<SecurityFinding> findings = new ArrayList<>();

        SecurityHeaders currentHeaders = getCurrentSecurityHeaders();

        // A04.1 & A05.1: Content Security Policy
        if (!currentHeaders.hasContentSecurityPolicy()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A04-001")
                .severity(SecuritySeverity.HIGH)
                .title("Missing Content Security Policy")
                .description("CSP header not configured")
                .recommendation("Implement strict CSP header")
                .owaspCategory("A04")
                .build());
        }

        // A05.2: X-Frame-Options
        if (!currentHeaders.hasFrameOptions()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A05-002")
                .severity(SecuritySeverity.MEDIUM)
                .title("Missing X-Frame-Options")
                .description("Clickjacking protection not configured")
                .recommendation("Set X-Frame-Options: DENY or SAMEORIGIN")
                .owaspCategory("A05")
                .build());
        }

        // A05.3: HSTS
        if (!currentHeaders.hasStrictTransportSecurity()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A05-003")
                .severity(SecuritySeverity.HIGH)
                .title("Missing HSTS Header")
                .description("HTTP Strict Transport Security not configured")
                .recommendation("Configure HSTS with appropriate max-age")
                .owaspCategory("A05")
                .build());
        }

        return SecurityHeadersAssessment.builder()
            .findings(findings)
            .currentHeaders(currentHeaders)
            .recommendedHeaders(getRecommendedHeaders())
            .build();
    }

    @Configuration
    @EnableWebSecurity
    public class SecureWebSecurityConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                // Constitutional requirement: OAuth2/PKCE
                .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(authorization -> authorization
                        .authorizationRequestResolver(pkceAuthorizationRequestResolver())
                    )
                )

                // Session management (Constitutional: opaque tokens in Redis)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .sessionFixation().migrateSession()
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
                    .sessionRegistry(sessionRegistry())
                )

                // Security headers (OWASP A04/A05)
                .headers(headers -> headers
                    .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)
                        .includeSubdomains(true)
                        .preload(true)
                    )
                    .contentTypeOptions(Customizer.withDefaults())
                    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )

                // CSRF protection
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/api/webhooks/**") // Only for webhook endpoints
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/webhooks/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
                )

                .build();
        }

        // Constitutional requirement: PKCE implementation
        @Bean
        public OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver() {
            DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                    clientRegistrationRepository(),
                    "/oauth2/authorization"
                );

            resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                .additionalParameters(params -> {
                    // PKCE parameters
                    String codeVerifier = generateCodeVerifier();
                    String codeChallenge = generateCodeChallenge(codeVerifier);
                    params.put("code_challenge", codeChallenge);
                    params.put("code_challenge_method", "S256");
                    // Store code verifier in session for verification
                    storeCodeVerifier(codeVerifier);
                })
            );

            return resolver;
        }
    }

    private DefaultCredentialsAssessment scanDefaultCredentials() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A05.4: Check for default credentials
        List<DefaultCredentialUsage> defaultCredentials = findDefaultCredentials();
        defaultCredentials.forEach(credential -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A05-004")
                .severity(SecuritySeverity.CRITICAL)
                .title("Default Credentials Found")
                .description("Default credentials in use: " + credential.getService())
                .location(credential.getLocation())
                .recommendation("Change default credentials immediately")
                .owaspCategory("A05")
                .build());
        });

        // A05.5: Check for hardcoded secrets
        List<HardcodedSecret> hardcodedSecrets = findHardcodedSecrets();
        hardcodedSecrets.forEach(secret -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A05-005")
                .severity(SecuritySeverity.CRITICAL)
                .title("Hardcoded Secret")
                .description("Secret hardcoded in source: " + secret.getType())
                .location(secret.getLocation())
                .recommendation("Move secrets to secure configuration management")
                .owaspCategory("A05")
                .build());
        });

        return DefaultCredentialsAssessment.builder()
            .findings(findings)
            .build();
    }
}
```

### A06: Vulnerable Components & A08: Software Integrity Failures
```java
package com.platform.security.dependencies;

@Component
public class DependencySecurityScanner {

    public DependencySecurityAssessment scanDependencies() {
        return DependencySecurityAssessment.builder()
            .vulnerableComponents(scanVulnerableComponents())
            .licenseCompliance(validateLicenseCompliance())
            .integrityChecks(validateIntegrity())
            .supplyChainSecurity(validateSupplyChain())
            .build();
    }

    private VulnerableComponentsAssessment scanVulnerableComponents() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A06.1: Check for known vulnerable dependencies
        List<VulnerableDependency> vulnerableDeps = getVulnerableDependencies();
        vulnerableDeps.forEach(dep -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A06-001")
                .severity(mapCvssToSeverity(dep.getCvssScore()))
                .title("Vulnerable Dependency")
                .description("Vulnerable component: " + dep.getName() + " " + dep.getVersion())
                .cveId(dep.getCveId())
                .recommendation("Update to version " + dep.getFixedVersion())
                .owaspCategory("A06")
                .build());
        });

        // A06.2: Check for outdated dependencies
        List<OutdatedDependency> outdatedDeps = getOutdatedDependencies();
        outdatedDeps.forEach(dep -> {
            if (dep.isSecurityUpdate()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A06-002")
                    .severity(SecuritySeverity.MEDIUM)
                    .title("Outdated Security-Critical Dependency")
                    .description("Outdated dependency with security updates: " + dep.getName())
                    .recommendation("Update to latest secure version")
                    .owaspCategory("A06")
                    .build());
            }
        });

        return VulnerableComponentsAssessment.builder()
            .findings(findings)
            .totalDependencies(getAllDependencies().size())
            .vulnerableDependencies(vulnerableDeps.size())
            .build();
    }

    private IntegrityAssessment validateIntegrity() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A08.1: Verify dependency checksums
        List<DependencyIntegrityCheck> integrityChecks = performIntegrityChecks();
        integrityChecks.forEach(check -> {
            if (!check.isValid()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A08-001")
                    .severity(SecuritySeverity.HIGH)
                    .title("Dependency Integrity Failure")
                    .description("Checksum mismatch for: " + check.getDependencyName())
                    .recommendation("Verify dependency source and re-download")
                    .owaspCategory("A08")
                    .build());
            }
        });

        // A08.2: Check for unsigned dependencies
        List<UnsignedDependency> unsignedDeps = findUnsignedDependencies();
        unsignedDeps.forEach(dep -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A08-002")
                .severity(SecuritySeverity.MEDIUM)
                .title("Unsigned Dependency")
                .description("Dependency lacks digital signature: " + dep.getName())
                .recommendation("Use only signed dependencies from trusted sources")
                .owaspCategory("A08")
                .build());
        });

        return IntegrityAssessment.builder()
            .findings(findings)
            .integrityCheckResults(integrityChecks)
            .build();
    }

    // Gradle security configuration
    @Component
    public class GradleSecurityConfiguration {

        public void configureSecureBuild() {
            // Dependency verification
            configureDependencyVerification();

            // Repository security
            configureSecureRepositories();

            // Plugin security
            configureSecurePlugins();
        }

        private void configureDependencyVerification() {
            // gradle/verification-metadata.xml configuration
            // Enables checksum verification for all dependencies
        }

        private void configureSecureRepositories() {
            // Only use HTTPS repositories
            // Prefer Maven Central and other trusted repositories
            // Avoid custom repositories without proper security
        }
    }
}
```

### A07: Identification and Authentication Failures & A09: Security Logging
```java
package com.platform.security.authentication;

@Component
public class AuthenticationSecurityValidator {

    public AuthenticationSecurityAssessment validateAuthentication() {
        return AuthenticationSecurityAssessment.builder()
            .passwordPolicy(validatePasswordPolicy())
            .sessionManagement(validateSessionSecurity())
            .bruteForceProtection(validateBruteForceProtection())
            .accountLockout(validateAccountLockout())
            .securityLogging(validateSecurityLogging())
            .build();
    }

    private SecurityLoggingAssessment validateSecurityLogging() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A09.1: Verify comprehensive security logging
        SecurityLoggingCoverage coverage = assessLoggingCoverage();
        if (!coverage.isComplete()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A09-001")
                .severity(SecuritySeverity.MEDIUM)
                .title("Incomplete Security Logging")
                .description("Security events not fully logged: " + coverage.getMissingEvents())
                .recommendation("Implement comprehensive security event logging")
                .owaspCategory("A09")
                .build());
        }

        // A09.2: Check for sensitive data in logs
        List<SensitiveDataInLogs> sensitiveDataInLogs = scanLogsForSensitiveData();
        sensitiveDataInLogs.forEach(data -> {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A09-002")
                .severity(SecuritySeverity.HIGH)
                .title("Sensitive Data in Logs")
                .description("Sensitive data logged: " + data.getDataType())
                .location(data.getLogLocation())
                .recommendation("Implement PII redaction in logging")
                .owaspCategory("A09")
                .build());
        });

        return SecurityLoggingAssessment.builder()
            .findings(findings)
            .loggingCoverage(coverage)
            .build();
    }

    @Component
    @Slf4j
    public class SecurityEventLogger {

        private final AuditService auditService;

        // A09.3: Comprehensive security event logging
        @EventListener
        public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
            auditService.recordSecurityEvent(SecurityEvent.builder()
                .eventType(SecurityEventType.AUTHENTICATION_SUCCESS)
                .userId(extractUserId(event))
                .ipAddress(extractIpAddress(event))
                .userAgent(extractUserAgent(event))
                .timestamp(Instant.now())
                .severity(SecuritySeverity.INFO)
                .description("User authentication successful")
                .build());
        }

        @EventListener
        public void onAuthenticationFailure(AuthenticationFailureEvent event) {
            auditService.recordSecurityEvent(SecurityEvent.builder()
                .eventType(SecurityEventType.AUTHENTICATION_FAILURE)
                .ipAddress(extractIpAddress(event))
                .userAgent(extractUserAgent(event))
                .timestamp(Instant.now())
                .severity(SecuritySeverity.WARNING)
                .description("Authentication failed: " + event.getException().getMessage())
                .failureReason(event.getException().getMessage())
                .build());
        }

        @EventListener
        public void onSessionCreated(HttpSessionCreatedEvent event) {
            auditService.recordSecurityEvent(SecurityEvent.builder()
                .eventType(SecurityEventType.SESSION_CREATED)
                .sessionId(event.getSession().getId())
                .timestamp(Instant.now())
                .severity(SecuritySeverity.INFO)
                .description("New session created")
                .build());
        }

        // Constitutional requirement: PII redaction in logs
        private String redactSensitiveData(String data) {
            // Implement PII redaction logic
            return PiiRedactionService.redact(data);
        }
    }

    private BruteForceProtectionAssessment validateBruteForceProtection() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A07.1: Check for rate limiting
        if (!hasRateLimiting()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A07-001")
                .severity(SecuritySeverity.HIGH)
                .title("Missing Rate Limiting")
                .description("No rate limiting on authentication endpoints")
                .recommendation("Implement rate limiting with exponential backoff")
                .owaspCategory("A07")
                .build());
        }

        // A07.2: Check for CAPTCHA on repeated failures
        if (!hasCaptchaProtection()) {
            findings.add(SecurityFinding.builder()
                .vulnerability("OWASP-A07-002")
                .severity(SecuritySeverity.MEDIUM)
                .title("Missing CAPTCHA Protection")
                .description("No CAPTCHA protection for repeated login failures")
                .recommendation("Implement CAPTCHA after multiple failed attempts")
                .owaspCategory("A07")
                .build());
        }

        return BruteForceProtectionAssessment.builder()
            .findings(findings)
            .rateLimitingEnabled(hasRateLimiting())
            .captchaEnabled(hasCaptchaProtection())
            .build();
    }
}
```

### A10: Server-Side Request Forgery (SSRF)
```java
package com.platform.security.ssrf;

@Component
public class SsrfProtectionValidator {

    public SsrfAssessment validateSsrfProtection() {
        return SsrfAssessment.builder()
            .urlValidation(validateUrlValidation())
            .networkRestrictions(validateNetworkRestrictions())
            .webhookSecurity(validateWebhookSecurity())
            .build();
    }

    private UrlValidationAssessment validateUrlValidation() {
        List<SecurityFinding> findings = new ArrayList<>();

        // A10.1: Check for user-controlled URLs
        List<UserControlledUrlUsage> userUrlUsages = findUserControlledUrls();
        userUrlUsages.forEach(usage -> {
            if (!usage.hasProperValidation()) {
                findings.add(SecurityFinding.builder()
                    .vulnerability("OWASP-A10-001")
                    .severity(SecuritySeverity.HIGH)
                    .title("SSRF Vulnerability")
                    .description("User-controlled URL without validation: " + usage.getEndpoint())
                    .location(usage.getLocation())
                    .recommendation("Implement URL allowlist and validation")
                    .owaspCategory("A10")
                    .build());
            }
        });

        return UrlValidationAssessment.builder()
            .findings(findings)
            .userControlledUrls(userUrlUsages.size())
            .build();
    }

    @Component
    public class SecureHttpClient {

        private final RestTemplate restTemplate;
        private final Set<String> allowedHosts;

        public SecureHttpClient() {
            this.restTemplate = createSecureRestTemplate();
            this.allowedHosts = loadAllowedHosts();
        }

        public <T> ResponseEntity<T> secureGet(String url, Class<T> responseType) {
            validateUrl(url);
            return restTemplate.getForEntity(url, responseType);
        }

        private void validateUrl(String url) {
            try {
                URI uri = new URI(url);

                // A10.2: Validate scheme
                if (!isAllowedScheme(uri.getScheme())) {
                    throw new SsrfException("Disallowed URL scheme: " + uri.getScheme());
                }

                // A10.3: Validate host
                if (!isAllowedHost(uri.getHost())) {
                    throw new SsrfException("Disallowed host: " + uri.getHost());
                }

                // A10.4: Check for private IP ranges
                if (isPrivateIpAddress(uri.getHost())) {
                    throw new SsrfException("Private IP address not allowed: " + uri.getHost());
                }

            } catch (URISyntaxException e) {
                throw new SsrfException("Invalid URL format", e);
            }
        }

        private boolean isAllowedScheme(String scheme) {
            return "https".equals(scheme) || "http".equals(scheme);
        }

        private boolean isAllowedHost(String host) {
            return allowedHosts.contains(host) || allowedHosts.contains("*." + getDomain(host));
        }

        private boolean isPrivateIpAddress(String host) {
            try {
                InetAddress address = InetAddress.getByName(host);
                return address.isSiteLocalAddress() ||
                       address.isLoopbackAddress() ||
                       address.isLinkLocalAddress();
            } catch (UnknownHostException e) {
                return false;
            }
        }

        private RestTemplate createSecureRestTemplate() {
            HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);

            return new RestTemplate(factory);
        }
    }
}
```

## OWASP Compliance Reporting

### Compliance Dashboard
```java
@RestController
@RequestMapping("/api/admin/security/owasp")
@PreAuthorize("hasRole('ADMIN')")
public class OwaspComplianceController {

    private final OwaspComplianceService complianceService;

    @GetMapping("/assessment")
    public ResponseEntity<OwaspComplianceReport> getComplianceAssessment() {
        OwaspComplianceReport report = complianceService.generateComplianceReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/vulnerabilities")
    public ResponseEntity<List<SecurityFinding>> getVulnerabilities(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category) {

        List<SecurityFinding> findings = complianceService.getFindings(severity, category);
        return ResponseEntity.ok(findings);
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanResult> triggerSecurityScan() {
        ScanResult result = complianceService.performFullSecurityScan();
        return ResponseEntity.ok(result);
    }
}

@Service
public class OwaspComplianceService {

    public OwaspComplianceReport generateComplianceReport() {
        return OwaspComplianceReport.builder()
            .scanTimestamp(Instant.now())
            .overallScore(calculateOverallComplianceScore())
            .categoryScores(calculateCategoryScores())
            .criticalFindings(getCriticalFindings())
            .highFindings(getHighFindings())
            .mediumFindings(getMediumFindings())
            .lowFindings(getLowFindings())
            .complianceStatus(determineComplianceStatus())
            .recommendations(generateRecommendations())
            .trendData(getComplianceTrends())
            .build();
    }

    private OwaspComplianceStatus determineComplianceStatus() {
        int criticalCount = getCriticalFindings().size();
        int highCount = getHighFindings().size();

        if (criticalCount > 0) {
            return OwaspComplianceStatus.NON_COMPLIANT;
        } else if (highCount > 5) {
            return OwaspComplianceStatus.PARTIALLY_COMPLIANT;
        } else {
            return OwaspComplianceStatus.COMPLIANT;
        }
    }
}
```

---

**Agent Version**: 1.0.0
**OWASP Version**: Top 10 2021
**Constitutional Compliance**: Required

Use this agent for comprehensive OWASP Top 10 security compliance assessment, vulnerability scanning, and security validation while maintaining strict constitutional compliance and payment security standards.
