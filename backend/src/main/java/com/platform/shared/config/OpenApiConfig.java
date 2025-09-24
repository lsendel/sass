package com.platform.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI paymentPlatformOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Payment Platform API")
            .description("Comprehensive payment platform with OAuth2 authentication")
            .version("1.0.0")
            .contact(new Contact()
                .name("Payment Platform Team")
                .email("support@paymentplatform.com")))
        .addServersItem(new Server()
            .url("http://localhost:8082")
            .description("Development server"))
        .components(new Components()
            .addSecuritySchemes("sessionAuth", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("SESSION")))
        .addSecurityItem(new SecurityRequirement().addList("sessionAuth"));
  }
}
