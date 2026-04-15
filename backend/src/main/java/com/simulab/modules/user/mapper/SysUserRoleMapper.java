package com.simulab.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户角色关联表 Mapper。
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
