package com.testplatform.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行报告列表/摘要 VO
 * 携带 LEFT JOIN 出来的 caseName / envName 用于报告列表表格直接展示，避免前端二次查询
 *
 * 数据隔离：所有列表与详情查询在 SQL 层就限定 create_by 与 is_deleted=0
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReportVO {

    private Long id;
    private Long caseId;
    private Long projectId;
    private Long envId;
    private Long createBy;

    /** RUNNING / PASSED / FAILED */
    private String status;

    private Integer totalSteps;
    private Integer passedSteps;
    private Integer failedSteps;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDurationMs;

    private String executor;

    private LocalDateTime createTime;

    /** LEFT JOIN：用例名（用例已被删时为 null） */
    private String caseName;

    /** LEFT JOIN：环境名（环境已被删时为 null） */
    private String envName;
}
