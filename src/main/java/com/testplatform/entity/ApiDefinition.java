package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接口定义实体类
 * 对应数据库表 t_api_definition，存储项目下可复用的接口资产
 * 通过 project_id 关联项目，通过 create_by 冗余字段实现数据隔离（避免每次 join t_project）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_api_definition")
public class ApiDefinition {

    /**
     * 主键ID
     * 自增主键，唯一标识一条接口定义
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
     * 关联 t_user.id，冗余字段，用于数据隔离（加速查询）
     */
    private Long createBy;

    /**
     * 接口名称
     * 展示用名称，长度不超过100个字符
     */
    private String name;

    /**
     * 请求方法
     * 取值 GET / POST / PUT / DELETE / PATCH
     */
    private String method;

    /**
     * 接口路径
     * 不包含域名，必须以 / 开头，如 /api/users
     */
    private String path;

    /**
     * 默认请求头
     * JSON 对象字符串，如 {"Content-Type":"application/json"}
     */
    private String requestHeaders;

    /**
     * 默认请求体
     * 原始字符串，通常为 JSON
     */
    private String requestBody;

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
     * 0: 正常（数据有效）
     * 1: 删除（数据已删除，但仍在数据库中保留）
     */
    @TableLogic
    private Integer isDeleted;
}
