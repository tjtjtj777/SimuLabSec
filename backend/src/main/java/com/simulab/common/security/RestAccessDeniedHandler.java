package com.simulab.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.api.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
/**
 * 权限不足处理器（403）。
 * 触发场景：
 * - 用户已经完成认证（已登录）；
 * - 但访问的资源超出其权限范围（角色/权限不满足）。
 *
 * 与 401 的区别：
 * - 401：你是谁还没被确认（未认证）
 * - 403：你是谁已确认，但你不能访问（已认证但未授权）
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * Spring Security 在授权失败（AccessDenied）时回调该方法。
     */
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        // 403：表示权限不足
        response.setStatus(HttpStatus.FORBIDDEN.value());
        // 统一响应编码与媒体类型，保证前端稳定解析
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 统一 JSON 错误结构，前端可按 code 做统一提示与埋点
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure("FORBIDDEN", "无权限访问该资源"));
    }
}
