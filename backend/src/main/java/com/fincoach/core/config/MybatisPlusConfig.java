package com.fincoach.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Value("${app.db:mysql}")
    private String db;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        DbType type = "postgres".equalsIgnoreCase(db) || "postgresql".equalsIgnoreCase(db)
                ? DbType.POSTGRE_SQL
                : DbType.MYSQL;
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(type));
        return interceptor;
    }
}
