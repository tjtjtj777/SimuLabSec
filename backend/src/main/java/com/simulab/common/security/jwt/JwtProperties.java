package com.simulab.common.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "simulab.security.jwt")
/**
 * JWT 配置项绑定。
 * 对应 application.yml:
 * simulab.security.jwt.issuer
 * simulab.security.jwt.secret
 * simulab.security.jwt.access-token-expire-seconds
 */
public class JwtProperties {

    /**
     * 签发者（iss）。
     */
    private String issuer;

    /**
     * JWT HMAC 密钥（生产环境应通过环境变量注入，不应硬编码）。
     */
    private String secret;

    /**
     * Access Token 过期时间（秒）。
     */
    private long accessTokenExpireSeconds;
}
