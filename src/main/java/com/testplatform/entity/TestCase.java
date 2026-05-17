package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例实体类
 * 对应数据库表 t_test_case，存储测试用例（业务流）的基础信息
 * 通过 project_id 关联项目，create_by 用于数据隔离
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_test_case")
public class TestCase {

    /**
     * 主键ID
     * 自增主键，唯一标识一条测试用例
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     * 外键关联 t_project.id
     */
    private Long projectId;

    /**
     * 创建人
     * 关联 t_user.id，用于数据隔离
     */
    private Long createBy;

    /**
     * 用例名称/业务流名称
     * 长度不超过100个字符
     */
    private String caseName;

    /**
     * 业务流描述
     * 选填，最长 255 字符
     */
    private String description;

    /**
     * 创建时间
     * 插入时由 MyMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 插入和更新时由 MyMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     * 0: 正常，1: 已删除
     */
    @TableLogic
    private Integer isDeleted;
}
