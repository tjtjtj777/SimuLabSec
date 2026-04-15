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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
/**
 * 未认证入口处理器（401）。
 * 触发场景：
 * - 访问需要登录的接口，但请求里没有有效认证信息；
 * - token 缺失、非法、过期导致认证失败。
 *
 * 在前后端分离项目中，不应重定向到登录页，而应返回统一 JSON。
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * Spring Security 在判定“当前请求未认证”时会调用该方法。
     */
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        // 401：表示“未登录/认证失败”，而不是“无权限”
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // 统一响应编码与媒体类型，避免中文提示乱码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 使用统一响应体结构，便于前端统一处理登录失效逻辑
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure("UNAUTHORIZED", "登录已失效，请重新登录"));
    }
}
