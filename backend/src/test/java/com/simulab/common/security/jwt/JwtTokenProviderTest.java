package com.simulab.common.security.jwt;

import com.simulab.common.security.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("simulab-test");
        properties.setSecret("SimuLabJwtSecretKeyForStageOneDevelopment123456");
        properties.setAccessTokenExpireSeconds(3600);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        SecurityUser user = SecurityUser.builder()
            .userId(1L)
            .username("demo")
            .password("ignored")
            .displayName("Demo")
            .roles(List.of("ADMIN"))
            .build();

        String token = provider.generateAccessToken(user);
        SecurityUser parsed = provider.buildSecurityUser(token);

        Assertions.assertEquals("demo", parsed.getUsername());
        Assertions.assertEquals(1L, parsed.getUserId());
        Assertions.assertTrue(parsed.getRoles().contains("ADMIN"));
    }
}
