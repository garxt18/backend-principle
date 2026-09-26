package com.backendprinciple.playground.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

/**
 * Keeps one {@link TokenBucket} per key (an IP address or a user id). Idle buckets expire so memory
 * stays bounded even under a flood of distinct keys.
 */
public class RateLimiter {

    private final long capacity;
    private final long periodNanos;
    private final Cache<String, TokenBucket> buckets;

    public RateLimiter(long capacity, Duration period) {
        this.capacity = capacity;
        this.periodNanos = period.toNanos();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterAccess(period.multipliedBy(2))
                .build();
    }

    /** @return 0 when allowed, otherwise the number of seconds the caller should wait. */
    public long tryAcquire(String key) {
        long now = System.nanoTime();
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket(capacity, periodNanos, now));
        return bucket.tryConsume(now) ? 0 : Math.max(1, bucket.secondsUntilNextToken());
    }
}
