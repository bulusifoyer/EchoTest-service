package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行明细实体
 * 对应数据库表 t_execution_detail，记录一次执行内每个步骤的请求与响应快照
 *
 * 状态枚举（与 report.status 区分）：PASSED / FAILED
 *
 * 字段语义说明：
 *   - actualRequest  / actualResponse：完整摘要 JSON（含 method/url/headers/body 等），供前端弹窗排查
 *   - statusCode / requestMethod / requestUrl / stepOrder：从摘要中冗余出来的字段，供报告页排序与筛选
 *   - assertResult：断言结果详情 JSON，记录每条断言的 left/right/operator/passed
 *   - failReason  ：失败原因短文本，用于报告页表格快速展示
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_execution_detail")
public class ExecutionDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联报告ID
     */
    private Long reportId;

    /**
     * 关联步骤ID（t_test_step.id）
     */
    private Long stepId;

    /**
     * 执行序号（从 1 起步，与 step_order 保持一致；遇错即停时只写实际执行过的步骤）
     */
    private Integer stepOrder;

    /**
     * 实际请求方法（变量替换后）
     */
    private String requestMethod;

    /**
     * 实际请求 URL（变量替换后）
     */
    private String requestUrl;

    /**
     * 实际发送请求摘要（含 method/url/headers/body，JSON）
     */
    private String actualRequest;

    /**
     * 实际响应摘要（含 statusCode/headers/body/elapsedMs，JSON）
     */
    private String actualResponse;

    /**
     * HTTP 响应状态码（网络异常时为 0）
     */
    private Integer statusCode;

    /**
     * 断言结果详情（JSON 数组：每条断言的 type/expression/expected/operator/actual/passed/message）
     */
    private String assertResult;

    /**
     * 失败原因（网络异常 / 断言失败短描述）
     */
    private String failReason;

    /**
     * 单步结果：PASSED / FAILED
     */
    private String status;

    /**
     * 响应耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
