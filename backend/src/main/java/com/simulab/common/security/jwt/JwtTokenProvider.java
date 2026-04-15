package com.simulab.common.security.jwt;

import com.simulab.common.security.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
/**
 * JWT 令牌提供器。
 * 负责：
 * 1. 基于当前登录用户生成 Access Token；
 * 2. 解析并校验 Token，提取声明（Claims）；
 * 3. 将 Claims 还原为系统内部可识别的 SecurityUser。
 */
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成访问令牌（Access Token）。
     * 核心载荷包含：用户名、用户ID、显示名、角色列表。
     */
    public String generateAccessToken(SecurityUser securityUser) {
        Instant now = Instant.now();
        return Jwts.builder()
            // iss：签发者，便于区分不同系统签发的 token
            .issuer(jwtProperties.getIssuer())
            // sub：主题，通常使用用户名或用户唯一标识
            .subject(securityUser.getUsername())
            // 自定义业务声明：用于后续直接恢复登录态，减少查库
            .claim("userId", securityUser.getUserId())
            .claim("displayName", securityUser.getDisplayName())
            .claim("roles", securityUser.getRoles())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessTokenExpireSeconds())))
            // 使用 HMAC 密钥签名，防篡改
            .signWith(getSecretKey())
            .compact();
    }

    /**
     * 解析并校验 Token。
     * 若签名错误、格式错误、已过期，JJWT 会抛异常，由上层统一处理。
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 从 token 构建 SecurityUser，供 Spring Security 上下文使用。
     */
    public SecurityUser buildSecurityUser(String token) {
        Claims claims = parseClaims(token);
        Object rolesObject = claims.get("roles");
        // Claims 反序列化后的 roles 可能是 List<?>，这里统一转为 List<String>
        List<String> roles = rolesObject instanceof List<?> rawRoles
            ? rawRoles.stream().map(String::valueOf).toList()
            : List.of();

        return SecurityUser.builder()
            .userId(Long.valueOf(String.valueOf(claims.get("userId"))))
            .username(claims.getSubject())
            .displayName(String.valueOf(claims.get("displayName")))
            .password("")
            .roles(roles)
            .build();
    }

    /**
     * 生成用于签名与验签的密钥。
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
