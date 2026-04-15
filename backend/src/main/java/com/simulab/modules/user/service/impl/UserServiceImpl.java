package com.simulab.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.user.entity.SysRole;
import com.simulab.modules.user.entity.SysUser;
import com.simulab.modules.user.entity.SysUserRole;
import com.simulab.modules.user.mapper.SysRoleMapper;
import com.simulab.modules.user.mapper.SysUserMapper;
import com.simulab.modules.user.mapper.SysUserRoleMapper;
import com.simulab.modules.user.service.UserService;
import com.simulab.modules.user.vo.CurrentUserVo;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
/**
 * 用户领域服务实现。
 * 负责：
 * 1. 按用户名加载安全用户模型（供登录鉴权使用）；
 * 2. 查询当前登录用户的展示信息；
 * 3. 完成注册、默认角色绑定等用户初始化流程。
 */
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
        SysUserMapper sysUserMapper,
        SysRoleMapper sysRoleMapper,
        SysUserRoleMapper sysUserRoleMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    /**
     * 按用户名加载安全用户。
     * 登录时会调用本方法获取密码哈希和角色列表。
     */
    public SecurityUser loadSecurityUserByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username)
            .eq(SysUser::getDeleted, 0));
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        return SecurityUser.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .displayName(user.getDisplayName())
            .roles(sysUserMapper.findRoleCodesByUserId(user.getId()))
            .build();
    }

    @Override
    /**
     * 获取“当前用户”信息（给 /api/users/me 等接口使用）。
     */
    public CurrentUserVo getCurrentUser(String username) {
        SecurityUser securityUser = loadSecurityUserByUsername(username);
        return CurrentUserVo.builder()
            .userId(securityUser.getUserId())
            .username(securityUser.getUsername())
            .displayName(securityUser.getDisplayName())
            .roles(securityUser.getRoles())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 注册新用户并分配默认 USER 角色。
     * 使用事务确保“创建用户 + 绑定角色”要么全部成功，要么全部回滚。
     */
    public CurrentUserVo register(String username, String password, String displayName) {
        validateRegisterPayload(username, password, displayName);
        // 检查用户名是否已被占用（仅统计未逻辑删除用户）
        boolean usernameExists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username)
            .eq(SysUser::getDeleted, 0)) > 0;
        if (usernameExists) {
            throw new BusinessException("USERNAME_EXISTS", "用户名已存在");
        }

        SysUser user = new SysUser();
        // 生成可读的业务用户编码，便于外部系统或日志定位
        user.setUserCode("U-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        user.setUsername(username);
        // 密码永不明文入库，只保存加密后的哈希
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        user.setPreferredLanguage("en-US");
        user.setIsDemoAccount(0);
        user.setCreatedBy(0L);
        user.setUpdatedBy(0L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        user.setVersion(0);
        sysUserMapper.insert(user);

        // 默认给新用户绑定 USER 角色，确保最小可用权限
        SysRole userRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleCode, "USER")
            .eq(SysRole::getDeleted, 0));
        if (userRole == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "系统角色 USER 缺失");
        }
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        relation.setCreatedBy(user.getId());
        relation.setCreatedAt(LocalDateTime.now());
        sysUserRoleMapper.insert(relation);

        return CurrentUserVo.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .displayName(user.getDisplayName())
            .roles(java.util.List.of("USER"))
            .build();
    }

    /**
     * 注册参数二次校验。
     * 说明：虽然 Controller 层已使用 DTO 注解校验，这里保留服务层校验，
     * 目的是防止绕过 Web 层直接调用服务时写入非法数据。
     */
    private void validateRegisterPayload(String username, String password, String displayName) {
        if (!StringUtils.hasText(username) || username.length() < 4 || username.length() > 32) {
            throw new BusinessException("USERNAME_INVALID", "用户名长度需在 4-32 之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException("USERNAME_INVALID", "用户名仅支持字母、数字和下划线");
        }
        if (!StringUtils.hasText(displayName) || displayName.length() < 2 || displayName.length() > 64) {
            throw new BusinessException("DISPLAY_NAME_INVALID", "显示名称长度需在 2-64 之间");
        }
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 64) {
            throw new BusinessException("PASSWORD_INVALID", "密码长度需在 8-64 之间");
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new BusinessException("PASSWORD_INVALID", "密码至少包含字母和数字");
        }
    }
}
