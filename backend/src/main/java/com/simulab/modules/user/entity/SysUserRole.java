package com.simulab.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user_role")
/**
 * 用户-角色关联实体，对应表 sys_user_role。
 * 一条记录表示“某个用户拥有某个角色”。
 */
public class SysUserRole {

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 用户ID（关联 sys_user.id）。
     */
    private Long userId;

    /**
     * 角色ID（关联 sys_role.id）。
     */
    private Long roleId;

    /**
     * 创建人ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
