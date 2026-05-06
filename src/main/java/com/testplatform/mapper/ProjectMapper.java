package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目数据访问接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作
 * 同时提供基于 create_by 的数据隔离查询方法
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 根据项目ID和创建人ID查询项目
     * 用于数据隔离校验，确保用户只能操作自己的项目
     *
     * @param id       项目ID
     * @param createBy 创建人ID（当前登录用户ID）
     * @return 项目实体，如果不存在或无权限则返回 null
     */
    @Select("SELECT * FROM t_project WHERE id = #{id} AND create_by = #{createBy} AND is_deleted = 0")
    Project selectByIdAndCreateBy(@Param("id") Long id, @Param("createBy") Long createBy);

    /**
     * 根据创建人ID查询项目列表
     * 用于项目列表接口，实现数据隔离，只返回当前用户创建的项目
     *
     * @param createBy 创建人ID（当前登录用户ID）
     * @return 项目列表
     */
    @Select("SELECT * FROM t_project WHERE create_by = #{createBy} AND is_deleted = 0 ORDER BY update_time DESC")
    List<Project> selectListByCreateBy(@Param("createBy") Long createBy);

    /**
     * 检查项目名称是否已存在（在当前用户下）
     * 同一用户下不允许创建同名项目
     *
     * @param name     项目名称
     * @param createBy 创建人ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM t_project WHERE name = #{name} AND create_by = #{createBy} AND is_deleted = 0")
    boolean existsByNameAndCreateBy(@Param("name") String name, @Param("createBy") Long createBy);
}
