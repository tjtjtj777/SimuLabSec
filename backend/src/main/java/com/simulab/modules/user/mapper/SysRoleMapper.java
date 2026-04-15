package com.simulab.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 角色表 Mapper。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
