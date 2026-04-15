package com.simulab.modules.user.service;

import com.simulab.common.security.SecurityUser;
import com.simulab.modules.user.vo.CurrentUserVo;

public interface UserService {

    /**
     * 按用户名加载安全用户（用于登录认证与 token 生成）。
     */
    SecurityUser loadSecurityUserByUsername(String username);

    /**
     * 获取当前用户展示信息。
     */
    CurrentUserVo getCurrentUser(String username);

    /**
     * 注册新用户并返回基础用户信息。
     */
    CurrentUserVo register(String username, String password, String displayName);
}
