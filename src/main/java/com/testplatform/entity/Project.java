package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目实体类
 * 对应数据库表 t_project，存储系统项目基础信息
 * 包含数据隔离字段 create_by，确保用户只能操作自己的项目
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_project")
public class Project {

    /**
     * 主键ID
     * 自增主键，唯一标识一个项目
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目名称
     * 项目的展示名称，长度不超过100个字符
     */
    private String name;

    /**
     * 项目描述
     * 对项目的详细说明，可选填
     */
    private String description;

    /**
     * 创建人
     * 关联 t_user.id，用于数据隔离，确保用户只能查看和操作自己创建的项目
     */
    private Long createBy;

    /**
     * 创建时间
     * 项目创建时间，由数据库或自动填充处理器赋值
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 项目信息最后修改时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     * 0: 正常（数据有效）
     * 1: 删除（数据已删除，但仍在数据库中保留）
     */
    @TableLogic
    private Integer isDeleted;
}
