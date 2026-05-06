package com.testplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 解决前后端分离项目中的跨域访问问题
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Configuration
public class CorsConfig {

    /**
     * 配置跨域过滤器
     * @return CorsFilter对象
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有域名进行跨域调用
        config.addAllowedOriginPattern("*");

        // 允许跨域发送cookie
        config.setAllowCredentials(true);

        // 放行全部原始头信息
        config.addAllowedHeader("*");

        // 允许所有请求方法进行跨域调用
        config.addAllowedMethod("*");

        // 设置预检请求的有效期（秒）
        // 在有效期内，浏览器不会重复发送预检请求
        config.setMaxAge(3600L);

        // 创建URL基础配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);

        // 返回CORS过滤器
        return new CorsFilter(source);
    }

    /**
     * 更严格的CORS配置（生产环境建议使用）
     * 只允许特定的域名进行跨域访问
     */
    /*
    @Bean
    public CorsFilter corsFilterStrict() {
        CorsConfiguration config = new CorsConfiguration();

        // 只允许特定的域名
        config.addAllowedOrigin("http://localhost:3000");  // Vue开发服务器
        config.addAllowedOrigin("https://testplatform.com");  // 生产域名
        config.addAllowedOrigin("https://www.testplatform.com");  // 生产域名

        // 允许跨域发送cookie
        config.setAllowCredentials(true);

        // 允许特定的请求头
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Accept-Language");
        config.addAllowedHeader("Accept-Encoding");
        config.addAllowedHeader("Access-Control-Request-Method");
        config.addAllowedHeader("Access-Control-Request-Headers");

        // 允许特定的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // 设置预检请求的有效期（秒）
        config.setMaxAge(3600L);

        // 暴露特定的响应头给前端
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
    */
}