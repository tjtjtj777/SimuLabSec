package com.simulab.modules.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityAuditLogger;
import com.simulab.common.security.SecurityProtectionService;
import com.simulab.common.security.SecurityUser;
import com.simulab.common.security.jwt.JwtProperties;
import com.simulab.common.security.jwt.JwtTokenProvider;
import com.simulab.modules.auth.dto.LoginRequest;
import com.simulab.modules.auth.dto.RegisterRequest;
import com.simulab.modules.auth.vo.LoginResponse;
import com.simulab.modules.user.service.UserService;
import com.simulab.modules.user.vo.CurrentUserVo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private SecurityProtectionService securityProtectionService;
    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Test
    void loginShouldReturnTokenWhenPasswordMatches() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpireSeconds(7200);
        AuthServiceImpl service = new AuthServiceImpl(
            userService, passwordEncoder, jwtTokenProvider, jwtProperties, securityProtectionService, securityAuditLogger);

        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("demo123456");

        SecurityUser user = SecurityUser.builder()
            .userId(1L)
            .username("demo")
            .password("{bcrypt}hash")
            .displayName("Demo Engineer")
            .roles(List.of("ADMIN"))
            .build();
        when(userService.loadSecurityUserByUsername("demo")).thenReturn(user);
        when(passwordEncoder.matches("demo123456", "{bcrypt}hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("jwt-token");

        LoginResponse response = service.login(request, "10.0.0.1");

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(7200L, response.getExpiresIn());
        assertEquals("demo", response.getUsername());
    }

    @Test
    void loginShouldThrowWhenPasswordNotMatch() {
        JwtProperties jwtProperties = new JwtProperties();
        AuthServiceImpl service = new AuthServiceImpl(
            userService, passwordEncoder, jwtTokenProvider, jwtProperties, securityProtectionService, securityAuditLogger);

        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("wrong");

        SecurityUser user = SecurityUser.builder()
            .username("demo")
            .password("{bcrypt}hash")
            .build();
        when(userService.loadSecurityUserByUsername("demo")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "{bcrypt}hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request, "10.0.0.1"));
        assertEquals("LOGIN_FAILED", ex.getCode());
        verify(securityProtectionService).recordLoginFailure("demo", "10.0.0.1", "password_mismatch");
    }

    @Test
    void registerShouldAutoLoginAfterUserCreated() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpireSeconds(7200);
        AuthServiceImpl service = new AuthServiceImpl(
            userService, passwordEncoder, jwtTokenProvider, jwtProperties, securityProtectionService, securityAuditLogger);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("abc12345");
        request.setDisplayName("New User");

        when(userService.register("new_user", "abc12345", "New User"))
            .thenReturn(CurrentUserVo.builder().userId(2L).username("new_user").displayName("New User").roles(List.of("USER")).build());
        SecurityUser securityUser = SecurityUser.builder()
            .userId(2L)
            .username("new_user")
            .password("{bcrypt}hash")
            .displayName("New User")
            .roles(List.of("USER"))
            .build();
        when(userService.loadSecurityUserByUsername("new_user")).thenReturn(securityUser);
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("register-token");

        LoginResponse response = service.register(request, "10.0.0.1");
        assertEquals("register-token", response.getAccessToken());
        assertEquals("new_user", response.getUsername());
        assertEquals(2L, response.getUserId());
    }

    @Test
    void registerShouldUseUsernameAsDefaultDisplayName() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpireSeconds(7200);
        AuthServiceImpl service = new AuthServiceImpl(
            userService, passwordEncoder, jwtTokenProvider, jwtProperties, securityProtectionService, securityAuditLogger);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("abc12345");

        when(userService.register("new_user", "abc12345", "new_user"))
            .thenReturn(CurrentUserVo.builder().userId(2L).username("new_user").displayName("new_user").roles(List.of("USER")).build());
        SecurityUser securityUser = SecurityUser.builder()
            .userId(2L)
            .username("new_user")
            .password("{bcrypt}hash")
            .displayName("new_user")
            .roles(List.of("USER"))
            .build();
        when(userService.loadSecurityUserByUsername("new_user")).thenReturn(securityUser);
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("register-token");

        service.register(request, "10.0.0.1");
        verify(userService).register("new_user", "abc12345", "new_user");
    }
}
