package com.simulab.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 注册请求参数。
 * 通过 Bean Validation 保证接口层参数基本合法，降低非法数据进入业务层的概率。
 */
public class RegisterRequest {

    /**
     * 账号用户名：4-32 位，仅字母/数字/下划线。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度需在 4-32 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
    private String username;

    /**
     * 登录密码：8-64 位，至少包含字母和数字。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码至少包含字母和数字")
    private String password;

    /**
     * 用户展示名。
     */
    @Size(min = 2, max = 64, message = "显示名称长度需在 2-64 之间")
    private String displayName;
}
