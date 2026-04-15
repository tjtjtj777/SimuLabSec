package com.simulab.modules.auth.service;

import com.simulab.modules.auth.dto.LoginRequest;
import com.simulab.modules.auth.dto.RegisterRequest;
import com.simulab.modules.auth.vo.LoginResponse;

public interface AuthService {

    /**
     * 登录：用户名密码校验成功后返回 token 与当前用户信息。
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /**
     * 注册：创建用户后返回 token 与当前用户信息。
     */
    LoginResponse register(RegisterRequest request, String clientIp);
}
