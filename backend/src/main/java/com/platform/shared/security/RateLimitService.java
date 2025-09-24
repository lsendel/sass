package com.platform.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Shared rate limiting service that keeps counters in Redis when available,
 * with an in-memory fallback for local development/tests.
 */
@Component
public class RateLimitService {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

  private final StringRedisTemplate redisTemplate;

  // Fallback store when Redis is not available
  private final ConcurrentHashMap<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

  public RateLimitService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    this.redisTemplate = redisTemplateProvider.getIfAvailable();
  }

  /**
   * Increments the counter for the supplied key and returns the total count within the window.
   */
  public long increment(String key, Duration window) {
    if (redisTemplate != null) {
      try {
        Long value = redisTemplate.opsForValue().increment(key, 1);
        if (value != null && value == 1) {
          redisTemplate.expire(key, window);
        }
        return value != null ? value : 0L;
      } catch (Exception ex) {
        logger.warn("Redis rate limiter unavailable, falling back to in-memory store", ex);
        return incrementLocal(key, window);
      }
    }

    return incrementLocal(key, window);
  }

  private long incrementLocal(String key, Duration window) {
    Instant now = Instant.now();
    LocalCounter counter =
        localCounters.compute(
            key,
            (k, existing) -> {
              if (existing == null || existing.windowExpiresAt.isBefore(now)) {
                return new LocalCounter(now.plus(window), new AtomicLong(1));
              }
              existing.count.incrementAndGet();
              return existing;
            });

    performLocalCleanup(now);
    return counter.count.get();
  }

  private void performLocalCleanup(Instant now) {
    for (Map.Entry<String, LocalCounter> entry : localCounters.entrySet()) {
      if (entry.getValue().windowExpiresAt.isBefore(now)) {
        localCounters.remove(entry.getKey(), entry.getValue());
      }
    }
  }

  private static final class LocalCounter {
    final Instant windowExpiresAt;
    final AtomicLong count;

    LocalCounter(Instant windowExpiresAt, AtomicLong count) {
      this.windowExpiresAt = windowExpiresAt;
      this.count = count;
    }
  }
}

