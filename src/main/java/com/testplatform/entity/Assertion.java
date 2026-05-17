package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 断言规则实体类
 * 对应数据库表 t_assertion
 * 用于在某一步骤执行后校验结果是否符合预期
 *
 * MVP 阶段支持的 assertType：
 *   - STATUS_CODE：断言 HTTP 状态码（expression 留空或填 status）
 *   - JSON_PATH：按 JSONPath 取值后断言（expression 填 JSONPath）
 * DATABASE 类型为规划项，本期不实现
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_assertion")
public class Assertion {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属步骤ID
     * 外键关联 t_test_step.id
     */
    private Long stepId;

    /**
     * 断言类型
     * MVP 仅支持 STATUS_CODE / JSON_PATH
     */
    private String assertType;

    /**
     * 断言表达式
     * STATUS_CODE 类型可留空；JSON_PATH 类型存放 JSONPath
     */
    private String expression;

    /**
     * 期望值
     */
    private String expectedValue;

    /**
     * 比较运算符
     * EQUALS / CONTAINS / GREATER_THAN（默认 EQUALS）
     */
    private String operator;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
