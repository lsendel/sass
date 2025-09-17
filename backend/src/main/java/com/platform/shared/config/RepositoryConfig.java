package com.platform.shared.config;

import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = {
      "com.platform.user.internal",
      "com.platform.auth.internal",
      "com.platform.payment.internal",
      "com.platform.subscription.internal",
      "com.platform.audit.internal"
    })
@Import(RedisRepositoriesAutoConfiguration.class)
public class RepositoryConfig {}
