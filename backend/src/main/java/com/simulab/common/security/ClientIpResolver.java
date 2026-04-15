package com.simulab.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            String first = xForwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return normalize(first);
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return normalize(realIp.trim());
        }
        return normalize(request.getRemoteAddr());
    }

    private static String normalize(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "unknown";
        }
        String normalized = ip.toLowerCase(Locale.ROOT);
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
