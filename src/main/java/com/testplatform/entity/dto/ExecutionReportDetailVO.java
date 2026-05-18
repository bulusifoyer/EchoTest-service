package com.testplatform.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行报告详情 VO（报告主表 + 明细列表的聚合结构）
 * 用于 GET /api/executions/{reportId} 一次性返回，避免前端再次发起明细请求
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReportDetailVO {

    /**
     * 报告主表（含 caseName / envName 冗余）
     */
    private ExecutionReportVO report;

    /**
     * 明细列表（按 stepOrder ASC）
     */
    private List<ExecutionDetailVO> details;
}
