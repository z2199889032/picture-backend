package com.zzm.picturebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置跨域请求的处理
 * 跨域资源共享（CORS）配置类，用于解决不同域之间的Ajax请求问题
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 添加跨域映射配置
     * 此方法配置了跨域请求的规则，使服务能够接受来自不同域的请求
     *
     * @param registry 跨域请求的注册表，用于构建跨域请求的规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
