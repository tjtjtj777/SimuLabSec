package com.simulab.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
/**
 * 系统用户实体，对应表 sys_user。
 */
public class SysUser extends BaseEntity {

    /**
     * 业务用户编码（可读标识，不等于主键ID）。
     */
    private String userCode;

    /**
     * 登录用户名（唯一）。
     */
    private String username;

    /**
     * 密码哈希值（非明文）。
     */
    private String passwordHash;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 邮箱（可选）。
     */
    private String email;

    /**
     * 账号状态，如 ACTIVE / DISABLED。
     */
    private String status;

    /**
     * 用户偏好语言。
     */
    private String preferredLanguage;

    /**
     * 是否演示账号：1-是，0-否。
     */
    private Integer isDemoAccount;
}
