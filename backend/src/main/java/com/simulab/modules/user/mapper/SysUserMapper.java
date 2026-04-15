package com.simulab.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.user.entity.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * 用户表 Mapper。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按用户ID查询角色编码列表。
     * 用于构建 SecurityUser 的 roles，参与 JWT 生成与接口授权。
     */
    @Select("""
        SELECT r.role_code
        FROM sys_role r
        JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
        """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
