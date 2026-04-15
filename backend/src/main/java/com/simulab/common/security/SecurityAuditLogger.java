package com.simulab.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logLoginAttempt(String username, String ip) {
        log.info("[security-audit] action=login_attempt username={} ip={}", username, ip);
    }

    public void logLoginSuccess(String username, Long userId, String ip) {
        log.info("[security-audit] action=login_success username={} userId={} ip={}", username, userId, ip);
    }

    public void logLoginFailure(String username, String ip, long userFailCount, boolean locked, String reason) {
        log.warn(
            "[security-audit] action=login_failure username={} ip={} userFailCount={} locked={} reason={}",
            username, ip, userFailCount, locked, reason
        );
    }

    public void logRegisterAttempt(String username, String ip) {
        log.info("[security-audit] action=register_attempt username={} ip={}", username, ip);
    }

    public void logRegisterSuccess(String username, Long userId, String ip) {
        log.info("[security-audit] action=register_success username={} userId={} ip={}", username, userId, ip);
    }

    public void logRateLimitHit(String endpoint, Long userId, String ip, String dimension, long limit, long windowSec) {
        log.warn(
            "[security-audit] action=rate_limit_hit endpoint={} userId={} ip={} dimension={} limit={} windowSec={}",
            endpoint, userId, ip, dimension, limit, windowSec
        );
    }
}
