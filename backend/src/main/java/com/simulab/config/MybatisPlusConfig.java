package com.simulab.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.simulab.common.security.SecurityContextUtils;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Long currentUserId = SecurityContextUtils.currentUserIdOrSystem();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
                strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId);
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
                strictInsertFill(metaObject, "version", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long currentUserId = SecurityContextUtils.currentUserIdOrSystem();
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
            }
        };
    }
}
