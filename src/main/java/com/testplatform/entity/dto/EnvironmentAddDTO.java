package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 环境配置新增数据传输对象
 * 用于接收前端为项目创建环境配置的请求参数
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
public class EnvironmentAddDTO {

    /**
     * 关联项目ID
     * 必填项，标识该环境配置所属的项目
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 环境名称
     * 必填项，如：开发环境、测试环境、生产环境
     */
    @NotBlank(message = "环境名称不能为空")
    @Size(max = 50, message = "环境名称长度不能超过50位")
    private String envName;

    /**
     * 根路径
     * 必填项，该环境的基础请求地址，如 http://api.test.com
     */
    @NotBlank(message = "根路径不能为空")
    @Size(max = 255, message = "根路径长度不能超过255位")
    private String baseUrl;

    /**
     * 全局请求头
     * 选填项，JSON格式，如 {"Content-Type":"application/json"}
     */
    private String globalHeaders;
}
