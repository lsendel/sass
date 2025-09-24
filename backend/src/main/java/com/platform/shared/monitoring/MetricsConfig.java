package com.platform.shared.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class MetricsConfig {

    @Bean
    public OncePerRequestFilter metricsFilter(MeterRegistry meterRegistry) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                Timer.Sample sample = Timer.start(meterRegistry);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    sample.stop(Timer.builder("http.requests")
                        .tag("method", request.getMethod())
                        .tag("status", String.valueOf(response.getStatus()))
                        .tag("uri", request.getRequestURI())
                        .register(meterRegistry));
                }
            }
        };
    }
}
