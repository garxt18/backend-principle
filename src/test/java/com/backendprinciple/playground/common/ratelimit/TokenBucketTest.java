package com.backendprinciple.playground.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private static final long MINUTE = TimeUnit.MINUTES.toNanos(1);

    @Test
    void allowsBurstUpToCapacityThenBlocks() {
        TokenBucket bucket = new TokenBucket(3, MINUTE, 0);
        assertThat(bucket.tryConsume(0)).isTrue();
        assertThat(bucket.tryConsume(0)).isTrue();
        assertThat(bucket.tryConsume(0)).isTrue();
        assertThat(bucket.tryConsume(0)).isFalse();
        assertThat(bucket.secondsUntilNextToken()).isEqualTo(20); // 3 per minute -> one every 20 s
    }

    @Test
    void refillsContinuously() {
        TokenBucket bucket = new TokenBucket(3, MINUTE, 0);
        for (int i = 0; i < 3; i++) {
            bucket.tryConsume(0);
        }
        assertThat(bucket.tryConsume(TimeUnit.SECONDS.toNanos(19))).isFalse();
        assertThat(bucket.tryConsume(TimeUnit.SECONDS.toNanos(21))).isTrue();
    }

    @Test
    void neverExceedsCapacityAfterLongIdle() {
        TokenBucket bucket = new TokenBucket(2, MINUTE, 0);
        long later = TimeUnit.HOURS.toNanos(5);
        assertThat(bucket.tryConsume(later)).isTrue();
        assertThat(bucket.tryConsume(later)).isTrue();
        assertThat(bucket.tryConsume(later)).isFalse();
    }
}
