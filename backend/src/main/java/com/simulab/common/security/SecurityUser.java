package com.simulab.common.security;

import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Builder
/**
 * 系统安全上下文中的用户模型。
 * 该对象实现 UserDetails，是 Spring Security 识别“当前登录用户”的标准载体。
 */
public class SecurityUser implements UserDetails {

    private Long userId;

    private String username;

    private String password;

    private String displayName;

    @Builder.Default
    private List<String> roles = List.of();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 统一把业务角色转换为 Spring Security 约定的 ROLE_ 前缀权限
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        // MVP 阶段暂不实现“账户过期”策略，默认不过期
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // MVP 阶段暂不实现“账户锁定”策略，默认不锁定
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // MVP 阶段暂不实现“凭证过期”策略，默认不过期
        return true;
    }

    @Override
    public boolean isEnabled() {
        // MVP 阶段暂不实现“禁用用户”策略，默认启用
        return true;
    }
}
