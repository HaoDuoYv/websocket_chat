package com.chat.config;

import com.chat.interceptor.AdminIpInterceptor;
import com.chat.interceptor.AdminSessionInterceptor;
import com.chat.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminIpInterceptor adminIpInterceptor;

    @Autowired
    private AdminSessionInterceptor adminSessionInterceptor;

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 鉴权拦截
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/auth/register", "/auth/login", "/files/**");

        // 管理员接口IP白名单拦截
        registry.addInterceptor(adminIpInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/health");

        registry.addInterceptor(adminSessionInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/health", "/api/admin/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/apps")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}
