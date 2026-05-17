package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试步骤实体类
 * 对应数据库表 t_test_step，存储用例下的步骤编排
 * 步骤是用例的子表，归属隔离通过父用例传递；步骤本身不软删，跟随用例生命周期物理清理
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_test_step")
public class TestStep {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用例ID
     * 外键关联 t_test_case.id
     */
    private Long caseId;

    /**
     * 引用接口ID
     * 外键关联 t_api_definition.id；引用前需校验该接口归属当前用户
     */
    private Long apiId;

    /**
     * 执行顺序
     * 后端按数组顺序自动从 1 开始重排，前端传入值会被忽略
     */
    private Integer stepOrder;

    /**
     * 步骤别名
     * 选填，长度不超过100个字符
     */
    private String stepName;

    /**
     * 覆盖请求体
     * 选填，支持 ${var} 变量引用（M3 执行引擎处理）
     */
    private String overrideRequestBody;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
