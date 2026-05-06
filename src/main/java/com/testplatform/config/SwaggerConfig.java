package com.testplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger2 配置类
 * 用于生成API文档，方便前后端开发人员查看和测试接口
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Configuration  // 声明为配置类
@EnableSwagger2  // 启用Swagger2
public class SwaggerConfig {

    /**
     * 创建API文档配置
     * @return Docket对象
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                // API基本信息
                .apiInfo(apiInfo())
                // 选择哪些接口暴露给Swagger
                .select()
                // 扫描的包路径
                .apis(RequestHandlerSelectors.basePackage("com.testplatform.controller"))
                // 匹配所有路径
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 配置API文档的基本信息
     * @return API信息对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 文档标题
                .title("测试平台API文档")
                // 文档描述
                .description("基于Spring Boot的分层自动化接口测试平台API文档")
                // 文档版本
                .version("1.0.0")
                // 联系人信息
                .contact(new Contact(
                        "测试平台开发团队",
                        "https://github.com/testplatform",
                        "dev@testplatform.com"))
                // 许可证
                .license("Apache 2.0")
                // 许可证URL
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html")
                // 服务条款URL
                .termsOfServiceUrl("https://github.com/testplatform/terms")
                .build();
    }

    /**
     * TODO: 后续可以添加以下配置
     * 1. 全局参数配置（如JWT令牌）
     * 2. 全局响应消息配置
     * 3. 分组API配置
     * 4. 安全方案配置
     */
}