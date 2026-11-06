package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.common.RateLimitException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final AiProperties properties;
    private final ConcurrentHashMap<Long, UserQuota> quotas = new ConcurrentHashMap<>();

    public AiRateLimiter(AiProperties properties) {
        this.properties = properties;
    }

    public void acquire(Long userId) {
        UserQuota quota = quotas.computeIfAbsent(userId, ignored -> new UserQuota(Instant.now()));
        synchronized (quota) {
            Instant now = Instant.now();
            if (now.isAfter(quota.windowStart.plus(WINDOW))) {
                quota.windowStart = now;
                quota.requests = 0;
            }
            if (quota.active >= properties.getMaxConcurrent()) {
                throw new RateLimitException("上一条 AI 回复仍在生成，请稍候");
            }
            if (quota.requests >= properties.getMaxRequestsPerHour()) {
                throw new RateLimitException("本小时 AI 请求次数已达上限，请稍后再试");
            }
            quota.active++;
            quota.requests++;
        }
    }

    public void release(Long userId) {
        UserQuota quota = quotas.get(userId);
        if (quota == null) {
            return;
        }
        synchronized (quota) {
            quota.active = Math.max(0, quota.active - 1);
        }
    }

    private static final class UserQuota {

        private Instant windowStart;
        private int requests;
        private int active;

        private UserQuota(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
