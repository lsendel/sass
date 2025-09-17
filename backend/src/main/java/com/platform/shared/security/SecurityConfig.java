package com.platform.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Public endpoints
    String[] publicEndpoints =
        new String[] {
          "/actuator/health", "/api/v1/auth/**", "/api/v1/plans/**", "/api/v1/webhooks/**"
        };

    http
        // APIs typically disable CSRF; adjust if you use cookies for non-API
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(publicEndpoints).permitAll().anyRequest().authenticated())
        // If OAuth2 login is used; keep default customizer (adjust as needed)
        .oauth2Login(Customizer.withDefaults())
        .addFilterAfter(new TenantContextFilter(), SecurityContextHolderFilter.class);

    return http.build();
  }
}
