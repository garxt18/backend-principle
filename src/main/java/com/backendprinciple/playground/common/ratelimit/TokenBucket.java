package com.backendprinciple.playground.common.ratelimit;

/**
 * Classic token-bucket rate limiter (the same algorithm the roadmap's "Rate Limiter" system design
 * exercise asks for). The bucket holds up to {@code capacity} tokens and refills continuously at
 * {@code capacity / periodNanos}. Each request takes one token; an empty bucket means "slow down".
 *
 * <p>This is an in-memory, single-instance limiter. With several app instances you would keep the
 * bucket in Redis (Level 8) so all instances share one count.
 */
final class TokenBucket {

    private final long capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefill;

    TokenBucket(long capacity, long periodNanos, long nowNanos) {
        this.capacity = capacity;
        this.refillPerNano = (double) capacity / periodNanos;
        this.tokens = capacity;
        this.lastRefill = nowNanos;
    }

    synchronized boolean tryConsume(long nowNanos) {
        long elapsed = Math.max(0, nowNanos - lastRefill);
        tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
        lastRefill = nowNanos;
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    /** Seconds until one token is available - sent to clients as Retry-After. */
    synchronized long secondsUntilNextToken() {
        double missing = 1 - tokens;
        return missing <= 0 ? 0 : (long) Math.ceil(missing / refillPerNano / 1_000_000_000d);
    }
}
