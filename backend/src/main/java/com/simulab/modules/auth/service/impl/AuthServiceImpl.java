package com.simulab.modules.auth.service.impl;

import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityAuditLogger;
import com.simulab.common.security.SecurityProtectionService;
import com.simulab.common.security.SecurityUser;
import com.simulab.common.security.jwt.JwtProperties;
import com.simulab.common.security.jwt.JwtTokenProvider;
import com.simulab.modules.auth.dto.LoginRequest;
import com.simulab.modules.auth.dto.RegisterRequest;
import com.simulab.modules.auth.service.AuthService;
import com.simulab.modules.auth.vo.LoginResponse;
import com.simulab.modules.user.service.UserService;
import com.simulab.modules.user.vo.CurrentUserVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * 认证服务实现。
 * 职责：
 * - 执行登录口令校验；
 * - 完成注册后自动登录；
 * - 统一组装登录响应（token + 用户基础信息）。
 */
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SecurityProtectionService securityProtectionService;
    private final SecurityAuditLogger securityAuditLogger;

    public AuthServiceImpl(
        UserService userService,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        JwtProperties jwtProperties,
        SecurityProtectionService securityProtectionService,
        SecurityAuditLogger securityAuditLogger
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.securityProtectionService = securityProtectionService;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    /**
     * 登录流程：
     * 1. 按用户名加载用户安全模型；
     * 2. 校验明文密码与加密密码是否匹配；
     * 3. 签发 access token；
     * 4. 返回登录响应。
     */
    public LoginResponse login(LoginRequest request, String clientIp) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        securityProtectionService.ensureLoginAllowed(username, clientIp);
        try {
            SecurityUser securityUser = userService.loadSecurityUserByUsername(username);
            if (!passwordEncoder.matches(request.getPassword(), securityUser.getPassword())) {
                securityProtectionService.recordLoginFailure(username, clientIp, "password_mismatch");
                throw new BusinessException("LOGIN_FAILED", "用户名或密码错误");
            }
            securityProtectionService.clearLoginFailureState(username);
            securityAuditLogger.logLoginSuccess(username, securityUser.getUserId(), clientIp);
            return buildLoginResponse(securityUser);
        } catch (BusinessException ex) {
            if ("USER_NOT_FOUND".equals(ex.getCode())) {
                securityProtectionService.recordLoginFailure(username, clientIp, "user_not_found");
                throw new BusinessException("LOGIN_FAILED", "用户名或密码错误");
            }
            throw ex;
        }
    }

    @Override
    /**
     * 注册并登录流程：
     * 1. 创建用户；
     * 2. 读取新用户的安全模型；
     * 3. 签发 access token；
     * 4. 返回登录响应。
     */
    public LoginResponse register(RegisterRequest request, String clientIp) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        securityProtectionService.ensureRegisterAllowed(username, clientIp);
        securityAuditLogger.logRegisterAttempt(username, clientIp);
        String displayName = StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName() : request.getUsername();
        CurrentUserVo createdUser = userService.register(username, request.getPassword(), displayName);
        SecurityUser securityUser = userService.loadSecurityUserByUsername(createdUser.getUsername());
        securityAuditLogger.logRegisterSuccess(securityUser.getUsername(), securityUser.getUserId(), clientIp);
        return buildLoginResponse(securityUser);
    }

    private LoginResponse buildLoginResponse(SecurityUser securityUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(securityUser);
        return LoginResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
            .userId(securityUser.getUserId())
            .username(securityUser.getUsername())
            .displayName(securityUser.getDisplayName())
            .roles(securityUser.getRoles())
            .build();
    }
}
