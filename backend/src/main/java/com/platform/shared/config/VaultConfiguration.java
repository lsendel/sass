package com.platform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;

/**
 * HashiCorp Vault configuration for secure secret management.
 * Used in production to securely store API keys, database passwords, etc.
 */
@Configuration
@Profile("!test")
@ConfigurationProperties(prefix = "vault")
public class VaultConfiguration extends AbstractVaultConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VaultConfiguration.class);

    private String host = "localhost";
    private int port = 8200;
    private String scheme = "https";
    private String token;
    private String namespace;

    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.create(host, port);
        endpoint.setScheme(scheme);
        return endpoint;
    }

    @Override
    public TokenAuthentication clientAuthentication() {
        return new TokenAuthentication(token);
    }

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultTemplate template = new VaultTemplate(vaultEndpoint(), clientAuthentication());
        if (namespace != null && !namespace.isEmpty()) {
            // Initialize KV v2 mount if needed - skip automatic mount creation
            logger.debug("Vault namespace configured: {}", namespace);
        }
        return template;
    }

    // Getters and setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}