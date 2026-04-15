package com.simulab.common.security;

import com.simulab.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SecurityProtectionServiceTest {

    @Test
    void ensureLoginAllowedShouldRejectWhenUsernameRateExceeded() {
        SecurityProtectionService service = new SecurityProtectionService(
            new SecurityThrottleService(null),
            new SecurityAuditLogger()
        );
        for (int i = 0; i < 12; i++) {
            service.ensureLoginAllowed("alice", "10.0.0.1");
        }

        BusinessException ex = Assertions.assertThrows(
            BusinessException.class,
            () -> service.ensureLoginAllowed("alice", "10.0.0.1")
        );
        Assertions.assertEquals("RATE_LIMITED", ex.getCode());
    }

    @Test
    void ensureLoginAllowedShouldRejectWhenUserLocked() {
        SecurityProtectionService service = new SecurityProtectionService(
            new SecurityThrottleService(null),
            new SecurityAuditLogger()
        );
        for (int i = 0; i < 5; i++) {
            service.recordLoginFailure("bob", "10.0.0.2", "password_mismatch");
        }

        BusinessException ex = Assertions.assertThrows(
            BusinessException.class,
            () -> service.ensureLoginAllowed("bob", "10.0.0.2")
        );
        Assertions.assertEquals("LOGIN_LOCKED", ex.getCode());
    }

    @Test
    void guardHeatmapBatchShouldRejectWhenUserRateExceeded() {
        SecurityProtectionService service = new SecurityProtectionService(
            new SecurityThrottleService(null),
            new SecurityAuditLogger()
        );
        for (int i = 0; i < 12; i++) {
            service.guardHeatmapBatch("/api/overlay-results/heatmap/batch", 1001L, "10.0.0.3");
        }

        BusinessException ex = Assertions.assertThrows(
            BusinessException.class,
            () -> service.guardHeatmapBatch("/api/overlay-results/heatmap/batch", 1001L, "10.0.0.3")
        );
        Assertions.assertEquals("RATE_LIMITED", ex.getCode());
    }
}
