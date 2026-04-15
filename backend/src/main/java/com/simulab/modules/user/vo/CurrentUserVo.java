package com.simulab.modules.user.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * 当前登录用户信息返回对象。
 */
public class CurrentUserVo {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 展示名。
     */
    private String displayName;

    /**
     * 角色编码列表。
     */
    private List<String> roles;
}
