package com.simulab.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
/**
 * 角色实体，对应表 sys_role。
 */
public class SysRole extends BaseEntity {

    /**
     * 角色编码（如 ADMIN / USER）。
     */
    private String roleCode;

    /**
     * 角色名称（展示用）。
     */
    private String roleName;

    /**
     * 角色描述。
     */
    private String description;

    /**
     * 角色状态。
     */
    private String status;
}
