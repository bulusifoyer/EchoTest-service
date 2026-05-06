package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 环境配置实体类
 * 对应数据库表 t_environment，存储项目的多环境配置信息
 * 通过 project_id 与项目表关联
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_environment")
public class Environment {

    /**
     * 主键ID
     * 自增主键，唯一标识一个环境配置
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联项目ID
     * 外键关联 t_project.id，标识该环境配置属于哪个项目
     */
    private Long projectId;

    /**
     * 环境名称
     * 如：开发环境、测试环境、生产环境等
     */
    private String envName;

    /**
     * 根路径
     * 该环境的基础请求地址，如 http://api.test.com
     */
    private String baseUrl;

    /**
     * 全局请求头
     * 存储 JSON 格式的全局请求头配置，会在该环境下的所有接口请求中自动携带
     */
    private String globalHeaders;

    /**
     * 创建时间
     * 环境配置创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 环境配置最后修改时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
