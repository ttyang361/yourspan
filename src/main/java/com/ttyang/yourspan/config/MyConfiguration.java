package com.ttyang.yourspan.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Configuration
@MapperScan("com.ttyang.yourspan.mapper")
public class MyConfiguration {
    /**
     * 导入mybatisplus的分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}
