package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 变量提取规则实体类
 * 对应数据库表 t_variable_extract
 * 用于在某一步骤的响应中按 JSONPath 提取变量，供后续步骤通过 ${var} 引用
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_variable_extract")
public class VariableExtract {

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
     * 变量名
     * 后续步骤通过 ${variable_name} 引用
     */
    private String variableName;

    /**
     * JSONPath 表达式
     * 例如 $.data.token，从响应体中提取值
     */
    private String jsonPath;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
