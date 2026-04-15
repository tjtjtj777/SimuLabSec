package com.simulab.modules.user.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.user.service.UserService;
import com.simulab.modules.user.vo.CurrentUserVo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
/**
 * 用户接口控制器。
 * 当前提供“查询当前登录用户信息”接口，供前端初始化用户态使用。
 */
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    /**
     * 获取当前登录用户信息。
     * Authentication 由 Spring Security 在 JWT 认证成功后自动注入。
     */
    public ApiResponse<CurrentUserVo> currentUser(Authentication authentication) {
        return ApiResponse.success(userService.getCurrentUser(authentication.getName()));
    }
}
