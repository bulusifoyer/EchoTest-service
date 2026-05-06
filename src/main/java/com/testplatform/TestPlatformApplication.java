package com.testplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 * 测试平台应用的入口点
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@SpringBootApplication  // 声明为Spring Boot应用，自动配置Spring和Spring MVC
@MapperScan("com.testplatform.mapper")  // 扫描MyBatis Mapper接口
public class TestPlatformApplication {

    /**
     * 主方法，应用启动入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用
        SpringApplication.run(TestPlatformApplication.class, args);
        System.out.println("==========================================");
        System.out.println("=   测试平台应用启动成功！                =");
        System.out.println("=   访问地址：http://localhost:8080      =");
        System.out.println("=   API文档：http://localhost:8080/doc.html =");
        System.out.println("==========================================");
    }

    /**
     * TODO: 后续可以添加以下功能
     * 1. 初始化管理员账号
     * 2. 加载系统配置
     * 3. 启动定时任务
     * 4. 初始化缓存
     */
}