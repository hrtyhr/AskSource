package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRateLimiterTest {

    @Test
    void limitsConcurrentRequestsAndHourlyQuota() {
        AiProperties properties = new AiProperties();
        properties.setMaxConcurrent(1);
        properties.setMaxRequestsPerHour(2);
        AiRateLimiter limiter = new AiRateLimiter(properties);

        limiter.acquire(1L);
        assertThatThrownBy(() -> limiter.acquire(1L))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("仍在生成");

        limiter.release(1L);
        limiter.acquire(1L);
        limiter.release(1L);

        assertThatThrownBy(() -> limiter.acquire(1L))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("上限");

        assertThatCode(() -> limiter.acquire(2L)).doesNotThrowAnyException();
    }
}
