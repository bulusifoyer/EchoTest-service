package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 接口定义更新数据传输对象
 * 用于接收前端修改接口资产的请求参数
 * id 走 body（与 ProjectUpdateDTO 保持同款风格）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class ApiDefinitionUpdateDTO {

    /**
     * 接口ID
     * 必填项，标识要更新的目标接口
     */
    @NotNull(message = "接口ID不能为空")
    private Long id;

    /**
     * 所属项目ID
     * 必填项，项目必须归属当前登录用户
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 接口名称
     * 必填项，长度1-100位
     */
    @NotBlank(message = "接口名称不能为空")
    @Size(max = 100, message = "接口名称长度不能超过100位")
    private String name;

    /**
     * 请求方法
     * 必填项，枚举白名单
     */
    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "请求方法只支持 GET/POST/PUT/DELETE/PATCH")
    private String method;

    /**
     * 接口路径
     * 必填项，必须以 / 开头，长度1-255位
     */
    @NotBlank(message = "接口路径不能为空")
    @Size(max = 255, message = "接口路径长度不能超过255位")
    @Pattern(regexp = "^/.*", message = "接口路径必须以 / 开头")
    private String path;

    /**
     * 默认请求头
     * 选填项，JSON 对象字符串
     */
    private String requestHeaders;

    /**
     * 默认请求体
     * 选填项
     */
    private String requestBody;
}
