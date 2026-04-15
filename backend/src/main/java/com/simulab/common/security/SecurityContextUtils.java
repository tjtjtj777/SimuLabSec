package com.simulab.common.security;

import com.simulab.common.exception.BusinessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    /**
     * 获取当前登录用户ID；若不存在登录态则返回系统保底用户ID（0）。
     * 典型用于审计字段（createdBy/updatedBy）在匿名场景下的兜底写入。
     */
    public static Long currentUserIdOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        return 0L;
    }

    /**
     * 获取当前登录用户ID；若未登录则抛业务异常。
     * 典型用于必须登录才能执行的业务操作。
     */
    public static Long currentUserIdOrThrow() {
        Long userId = currentUserIdOrSystem();
        if (userId == 0L) {
            throw new BusinessException("UNAUTHORIZED", "未登录或登录已失效");
        }
        return userId;
    }
}
