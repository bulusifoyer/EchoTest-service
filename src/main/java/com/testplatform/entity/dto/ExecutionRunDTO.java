package com.testplatform.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 执行用例请求 DTO
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRunDTO {

    /**
     * 用例ID（必须属于当前用户）
     */
    @NotNull(message = "用例ID不能为空")
    private Long caseId;

    /**
     * 环境ID（必须属于当前用户，且与用例同项目）
     */
    @NotNull(message = "环境ID不能为空")
    private Long envId;

    /**
     * 单步超时（毫秒）；范围 [100, 60000]，默认 10000
     */
    @Min(value = 100, message = "超时时间最小为 100 毫秒")
    @Max(value = 60000, message = "超时时间最大为 60000 毫秒")
    private Integer timeoutMs;
}
