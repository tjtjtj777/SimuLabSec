package com.simulab.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
/**
 * 权限实体，对应表 sys_permission。
 * 当前阶段主要用于权限模型预留。
 */
public class SysPermission extends BaseEntity {

    /**
     * 权限编码（如 RECIPE_EDIT）。
     */
    private String permissionCode;

    /**
     * 权限名称。
     */
    private String permissionName;

    /**
     * 所属模块编码（如 RECIPE / TASK）。
     */
    private String moduleCode;

    /**
     * 权限描述。
     */
    private String description;
}
