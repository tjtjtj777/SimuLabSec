package com.simulab.common.security;

import com.simulab.common.exception.BusinessException;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class SecurityProtectionService {

    private static final Duration LOGIN_RATE_WINDOW = Duration.ofMinutes(1);
    private static final Duration REGISTER_RATE_WINDOW = Duration.ofMinutes(10);
    private static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(10);
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration HEAVY_ENDPOINT_WINDOW = Duration.ofMinutes(1);
    private static final long LOGIN_IP_LIMIT = 30L;
    private static final long LOGIN_USER_LIMIT = 12L;
    private static final long REGISTER_IP_LIMIT = 10L;
    private static final long REGISTER_USER_LIMIT = 3L;
    private static final long LOGIN_FAIL_USER_LIMIT = 5L;
    private static final long GENERATE_USER_LIMIT = 6L;
    private static final long GENERATE_IP_LIMIT = 20L;
    private static final long HEATMAP_USER_LIMIT = 60L;
    private static final long HEATMAP_IP_LIMIT = 180L;
    private static final long HEATMAP_BATCH_USER_LIMIT = 12L;
    private static final long HEATMAP_BATCH_IP_LIMIT = 40L;

    private final SecurityThrottleService throttleService;
    private final SecurityAuditLogger auditLogger;

    public SecurityProtectionService(SecurityThrottleService throttleService, SecurityAuditLogger auditLogger) {
        this.throttleService = throttleService;
        this.auditLogger = auditLogger;
    }

    public void ensureLoginAllowed(String username, String ip) {
        ensureNotLocked(username, ip);
        enforceCounter("auth:login:ip:" + ip, LOGIN_IP_LIMIT, LOGIN_RATE_WINDOW, "登录过于频繁，请稍后重试");
        enforceCounter("auth:login:user:" + username, LOGIN_USER_LIMIT, LOGIN_RATE_WINDOW, "账号登录尝试过于频繁，请稍后重试");
    }

    public void ensureRegisterAllowed(String username, String ip) {
        enforceCounter("auth:register:ip:" + ip, REGISTER_IP_LIMIT, REGISTER_RATE_WINDOW, "注册过于频繁，请稍后再试");
        enforceCounter("auth:register:user:" + username, REGISTER_USER_LIMIT, REGISTER_RATE_WINDOW, "该用户名注册尝试过于频繁，请稍后再试");
    }

    public long recordLoginFailure(String username, String ip, String reason) {
        long userFailCount = throttleService.incrementAndGet("auth:login:fail:user:" + username, LOGIN_FAIL_WINDOW);
        if (userFailCount >= LOGIN_FAIL_USER_LIMIT) {
            throttleService.block("auth:login:lock:user:" + username, LOGIN_LOCK_DURATION);
        }
        auditLogger.logLoginFailure(username, ip, userFailCount, userFailCount >= LOGIN_FAIL_USER_LIMIT, reason);
        return userFailCount;
    }

    public void clearLoginFailureState(String username) {
        throttleService.clear("auth:login:fail:user:" + username);
        throttleService.clear("auth:login:lock:user:" + username);
    }

    public void guardGenerate(String endpoint, Long userId, String ip) {
        guardHeavyEndpoint(endpoint, userId, ip, GENERATE_USER_LIMIT, GENERATE_IP_LIMIT);
    }

    public void guardHeatmap(String endpoint, Long userId, String ip) {
        guardHeavyEndpoint(endpoint, userId, ip, HEATMAP_USER_LIMIT, HEATMAP_IP_LIMIT);
    }

    public void guardHeatmapBatch(String endpoint, Long userId, String ip) {
        guardHeavyEndpoint(endpoint, userId, ip, HEATMAP_BATCH_USER_LIMIT, HEATMAP_BATCH_IP_LIMIT);
    }

    private void ensureNotLocked(String username, String ip) {
        String lockKey = "auth:login:lock:user:" + username;
        if (throttleService.isBlocked(lockKey)) {
            long ttlSec = throttleService.ttlSeconds(lockKey);
            throw new BusinessException("LOGIN_LOCKED", "登录失败次数过多，请在 " + Math.max(ttlSec, 1L) + " 秒后重试");
        }
        auditLogger.logLoginAttempt(username, ip);
    }

    private void guardHeavyEndpoint(String endpoint, Long userId, String ip, long userLimit, long ipLimit) {
        String userKey = "security:rate:user:" + endpoint + ":" + userId;
        String ipKey = "security:rate:ip:" + endpoint + ":" + ip;
        enforceCounterWithAudit(endpoint, userId, ip, "userId", userKey, userLimit, HEAVY_ENDPOINT_WINDOW);
        enforceCounterWithAudit(endpoint, userId, ip, "ip", ipKey, ipLimit, HEAVY_ENDPOINT_WINDOW);
    }

    private void enforceCounter(String key, long limit, Duration window, String errorMessage) {
        long current = throttleService.incrementAndGet(key, window);
        if (current > limit) {
            throw new BusinessException("RATE_LIMITED", errorMessage);
        }
    }

    private void enforceCounterWithAudit(
        String endpoint,
        Long userId,
        String ip,
        String dimension,
        String key,
        long limit,
        Duration window
    ) {
        long current = throttleService.incrementAndGet(key, window);
        if (current > limit) {
            auditLogger.logRateLimitHit(endpoint, userId, ip, dimension, limit, window.getSeconds());
            throw new BusinessException("RATE_LIMITED", "请求过于频繁，请稍后重试");
        }
    }
}
