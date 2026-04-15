package com.simulab.modules.auth.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.common.security.ClientIpResolver;
import com.simulab.modules.auth.dto.LoginRequest;
import com.simulab.modules.auth.dto.RegisterRequest;
import com.simulab.modules.auth.service.AuthService;
import com.simulab.modules.auth.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
/**
 * 认证控制器。
 * 对外提供登录、注册、退出等认证相关接口。
 */
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    /**
     * 用户登录：校验凭证后签发 access token。
     */
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success("登录成功", authService.login(request, ClientIpResolver.resolve(httpServletRequest)));
    }

    @PostMapping("/register")
    /**
     * 用户注册：创建账号后直接返回登录态（token）。
     */
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success("注册成功", authService.register(request, ClientIpResolver.resolve(httpServletRequest)));
    }

    @PostMapping("/logout")
    /**
     * 用户退出：JWT 无状态模式下，服务端不保存会话，主要用于前端交互语义统一。
     */
    public ApiResponse<Void> logout() {
        // JWT 无状态退出由前端清理本地 token；接口保留用于统一交互语义。
        return ApiResponse.success("退出成功", null);
    }
}
