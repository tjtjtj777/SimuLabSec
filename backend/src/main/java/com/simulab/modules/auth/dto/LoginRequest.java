package com.simulab.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 登录请求参数。
 */
public class LoginRequest {

    /**
     * 登录用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不能超过 32")
    private String username;

    /**
     * 登录密码（明文仅用于传输，服务端会进行加密比对）。
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;
}
