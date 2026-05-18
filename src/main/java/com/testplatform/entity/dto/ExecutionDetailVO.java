package com.testplatform.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行明细 VO
 * 与 ExecutionDetail 实体字段对齐，用于报告详情页步骤展开渲染
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDetailVO {

    private Long id;
    private Long reportId;
    private Long stepId;
    private Integer stepOrder;

    private String requestMethod;
    private String requestUrl;
    private String actualRequest;
    private String actualResponse;

    private Integer statusCode;
    private String assertResult;
    private String failReason;

    /** PASSED / FAILED */
    private String status;
    private Long elapsedMs;
    private LocalDateTime createTime;
}
