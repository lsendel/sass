package com.platform.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aspect-based rate limiting using annotations.
 * Provides fine-grained control over rate limits for specific endpoints.
 */
@Aspect
@Component
public class RateLimitingAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingAspect.class);

    @Autowired
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            // No HTTP context, proceed without rate limiting
            return joinPoint.proceed();
        }

        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String key = String.format("rate_limit:%s:%s:%s", rateLimited.keyPrefix(), clientIp, endpoint);

        Duration window = Duration.of(rateLimited.window(), rateLimited.unit().toChronoUnit());
        long currentCount = rateLimitService.increment(key, window);

        if (currentCount > rateLimited.requests()) {
            logger.warn("SECURITY_EVENT: Rate limit exceeded - IP: {}, Endpoint: {}, Count: {}, Limit: {}",
                clientIp, endpoint, currentCount, rateLimited.requests());

            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Please try again later."
            );
        }

        logger.debug("Rate limit check passed for IP: {}, Endpoint: {}, Count: {}/{}",
            clientIp, endpoint, currentCount, rateLimited.requests());

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Annotation for marking methods that should be rate limited
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimited {

        /**
         * Number of requests allowed within the time window
         */
        int requests() default 10;

        /**
         * Time window for rate limiting
         */
        long window() default 60;

        /**
         * Time unit for the window
         */
        java.util.concurrent.TimeUnit unit() default java.util.concurrent.TimeUnit.SECONDS;

        /**
         * Key prefix for distinguishing different types of rate limits
         */
        String keyPrefix() default "default";
    }
}