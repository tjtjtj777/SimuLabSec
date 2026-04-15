package com.simulab.config;

import com.simulab.common.security.RestAccessDeniedHandler;
import com.simulab.common.security.RestAuthenticationEntryPoint;
import com.simulab.common.security.jwt.JwtAuthenticationFilter;
import com.simulab.common.security.jwt.JwtProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
/**
 * Spring Security 总配置。
 * 目标：
 * - 采用 JWT 无状态认证；
 * - 统一处理未认证（401）与无权限（403）响应；
 * - 明确公开接口与受保护接口边界。
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final String allowedOrigins;
    private final String allowedMethods;
    private final String allowedHeaders;
    private final boolean allowCredentials;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
        RestAccessDeniedHandler restAccessDeniedHandler,
        @Value("${simulab.security.cors.allowed-origins:*}") String allowedOrigins,
        @Value("${simulab.security.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}") String allowedMethods,
        @Value("${simulab.security.cors.allowed-headers:*}") String allowedHeaders,
        @Value("${simulab.security.cors.allow-credentials:true}") boolean allowCredentials
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.allowCredentials = allowCredentials;
    }

    @Bean
    /**
     * 配置安全过滤链。
     */
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 前后端分离 + JWT 场景通常关闭 CSRF（不依赖 Cookie Session）
            .csrf(AbstractHttpConfigurer::disable)
            // 使用默认 CORS 处理，具体跨域来源可在其他配置中收敛
            .cors(Customizer.withDefaults())
            // 自定义异常返回，保证 API 始终输出统一 JSON 结构
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler))
            // 无状态会话：服务端不保存登录态，完全依赖 token
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 文档接口放行，便于联调与接口测试
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 登录/注册接口放行
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dashboard/overview").authenticated()
                // 其他接口默认都要求登录
                .anyRequest().authenticated())
            // JWT 过滤器提前执行，先尝试建立认证上下文
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 DelegatingPasswordEncoder，默认 bcrypt，兼容历史前缀密码格式。
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS 统一走配置项，方便生产环境按域名白名单收敛。
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(csvList(allowedOrigins));
        config.setAllowedMethods(csvList(allowedMethods));
        config.setAllowedHeaders(csvList(allowedHeaders));
        config.setAllowCredentials(allowCredentials);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> csvList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("*");
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toList());
    }
}
