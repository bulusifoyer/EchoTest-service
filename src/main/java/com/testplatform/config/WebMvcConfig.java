package com.testplatform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 * 注册自定义拦截器，配置拦截路径和排除路径
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    /**
     * 注册拦截器
     * JWT 认证拦截器会拦截所有 /api 开头的请求，但排除认证相关的公开接口
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                // 拦截所有 /api 路径下的请求
                .addPathPatterns("/api/**")
                // 排除认证相关接口（注册、登录、检查用户名/邮箱等公开接口）
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/check-username",
                        "/api/auth/check-email",
                        "/api/auth/refresh-token"
                )
                // 排除 Swagger 文档相关路径
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/api-docs"
                );
    }
}
