package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行报告主表实体
 * 对应数据库表 t_execution_report，存储一次用例执行的汇总信息
 *
 * 状态枚举（与 detail.status 区分）：
 *   - RUNNING：执行中（创建报告时即写入；同步串行的 MVP 几乎不会观察到）
 *   - PASSED ：所有步骤断言通过
 *   - FAILED ：任一步骤失败（网络异常 / 断言不通过），遇错即停后置为 FAILED
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_execution_report")
public class ExecutionReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 执行的用例ID（关联 t_test_case.id）
     */
    private Long caseId;

    /**
     * 所属项目ID（冗余，便于 LEFT JOIN 列表 + 数据隔离）
     */
    private Long projectId;

    /**
     * 创建人ID（关联 t_user.id，数据隔离）
     */
    private Long createBy;

    /**
     * 执行环境ID（关联 t_environment.id）
     */
    private Long envId;

    /**
     * 最终结果：RUNNING / PASSED / FAILED
     */
    private String status;

    /**
     * 用例总步骤数（不论是否实际执行到，都按用例 step 数量记）
     */
    private Integer totalSteps;

    /**
     * 通过步骤数
     */
    private Integer passedSteps;

    /**
     * 失败步骤数（遇错即停 → 最多为 1）
     */
    private Integer failedSteps;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 执行人（用户名/昵称展示用，便于报告页显示）
     */
    private String executor;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除（0:正常 1:删除）
     */
    @TableLogic
    private Integer isDeleted;
}
