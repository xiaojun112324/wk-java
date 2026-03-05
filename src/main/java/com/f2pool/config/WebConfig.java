package com.f2pool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 * 允许前端页面（包括本地文件 file://）访问后端接口
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有路径生效
                .allowedOriginPatterns("*") // 允许所有来源（包括 file://）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有方法
                .allowedHeaders("*") // 允许所有头信息
                .allowCredentials(true) // 允许携带 Cookie
                .maxAge(3600); // 预检请求缓存时间
    }
}
