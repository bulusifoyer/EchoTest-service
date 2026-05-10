package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.ApiDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 接口定义数据访问接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础 CRUD
 * 额外提供基于 create_by 的数据隔离查询方法
 *
 * 安全说明：所有动态条件均通过 #{} 参数绑定，method 字段在 DTO 层走 @Pattern 白名单，
 * 防止 SQL 注入。
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Mapper
public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {

    /**
     * 根据接口ID和创建人ID查询接口定义
     * 用于数据隔离校验，确保用户只能操作自己的接口
     *
     * @param id       接口ID
     * @param createBy 创建人ID（当前登录用户ID）
     * @return 接口定义实体，如果不存在或无权限则返回 null
     */
    @Select("SELECT * FROM t_api_definition " +
            "WHERE id = #{id} AND create_by = #{createBy} AND is_deleted = 0")
    ApiDefinition selectByIdAndCreateBy(@Param("id") Long id,
                                        @Param("createBy") Long createBy);

    /**
     * 查询指定项目下当前用户的所有接口定义
     * 按 update_time 降序排列，最近更新的排在前面
     *
     * @param projectId 项目ID
     * @param createBy  创建人ID（当前登录用户ID）
     * @return 接口定义列表
     */
    @Select("SELECT * FROM t_api_definition " +
            "WHERE project_id = #{projectId} AND create_by = #{createBy} AND is_deleted = 0 " +
            "ORDER BY update_time DESC")
    List<ApiDefinition> selectListByProjectAndCreateBy(@Param("projectId") Long projectId,
                                                       @Param("createBy") Long createBy);

    /**
     * 检查同一项目下是否已存在相同 (method, path) 的接口
     * 用于唯一性校验，避免同项目下登记重复接口
     *
     * @param projectId 项目ID
     * @param method    请求方法
     * @param path      接口路径
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM t_api_definition " +
            "WHERE project_id = #{projectId} AND method = #{method} AND path = #{path} " +
            "AND is_deleted = 0")
    boolean existsByProjectMethodPath(@Param("projectId") Long projectId,
                                      @Param("method") String method,
                                      @Param("path") String path);
}
