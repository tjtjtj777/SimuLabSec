package com.simulab.modules.auth.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * 登录/注册成功后的统一返回对象。
 */
public class LoginResponse {

    /**
     * 访问令牌（JWT）。
     */
    private String accessToken;

    /**
     * 令牌类型，当前固定为 Bearer。
     */
    private String tokenType;

    /**
     * 令牌有效期（秒）。
     */
    private Long expiresIn;

    /**
     * 当前登录用户ID。
     */
    private Long userId;

    /**
     * 当前登录用户名。
     */
    private String username;

    /**
     * 当前登录用户展示名。
     */
    private String displayName;

    /**
     * 当前登录用户角色列表。
     */
    private List<String> roles;
}
