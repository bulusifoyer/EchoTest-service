package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 接口试调数据传输对象
 * 用于前端发起"单接口在线试调"的请求参数
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class ApiTryRunDTO {

    /**
     * 要试调的接口ID
     * 必填项，接口必须归属当前登录用户
     */
    @NotNull(message = "接口ID不能为空")
    private Long apiId;

    /**
     * 使用的环境ID
     * 必填项，环境会提供 base_url 与 global_headers
     * 环境所属项目必须归属当前登录用户
     */
    @NotNull(message = "环境ID不能为空")
    private Long envId;

    /**
     * 本次调用临时覆盖的请求头
     * 选填项，JSON 对象字符串，优先级最高（覆盖环境与接口定义）
     */
    private String overrideHeaders;

    /**
     * 本次调用临时覆盖的请求体
     * 选填项，非空时使用；为 null 则使用接口定义的 requestBody
     */
    private String overrideBody;

    /**
     * 整体调用超时（毫秒）
     * 选填项，默认 10000ms；用于 OkHttp callTimeout
     */
    @Min(value = 100, message = "超时时间不能低于100毫秒")
    @Max(value = 60000, message = "超时时间不能超过60000毫秒")
    private Integer timeoutMs = 10000;
}
