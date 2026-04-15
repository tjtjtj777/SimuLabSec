package com.simulab.common.security.jwt;

import com.simulab.common.security.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
/**
 * JWT 鉴权过滤器。
 * 作用：
 * 1. 从请求头提取 Bearer Token；
 * 2. 校验并解析 Token，构造当前登录用户；
 * 3. 将认证信息写入 SecurityContext，供后续授权判断使用。
 *
 * 说明：
 * - 继承 OncePerRequestFilter，确保每个请求只执行一次，避免重复解析 token。
 * - 解析失败时不直接中断请求，而是清空上下文后继续交给后续链路，
 *   由 Spring Security 统一触发 401/403 处理器返回标准响应。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    /**
     * 每次 HTTP 请求都会进入该方法（一次）。
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // 从 Authorization 请求头读取认证信息，期望格式：Bearer <token>
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            // 去掉固定前缀 "Bearer "（长度 7），得到纯 token
            String token = authorization.substring(7);
            try {
                // 校验 token（签名、过期等）并恢复出业务用户对象
                SecurityUser securityUser = jwtTokenProvider.buildSecurityUser(token);
                // 把用户信息包装为 Spring Security 的认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    securityUser, null, securityUser.getAuthorities());
                // 附加请求来源信息（IP、SessionId 等，便于审计和排错）
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 写入上下文：后续控制器和权限判断都从这里拿“当前用户”
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // token 非法或过期时，清空上下文，表示当前请求未认证
                SecurityContextHolder.clearContext();
            }
        }
        // 无论是否认证成功，都继续执行后续过滤器与控制器逻辑
        filterChain.doFilter(request, response);
    }
}
