package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ProjectAddDTO;
import com.testplatform.entity.dto.ProjectUpdateDTO;

import java.util.List;

/**
 * 项目业务接口
 * 定义项目相关的业务逻辑规范，包括项目的增删改查
 * 所有操作均需进行数据隔离（基于 create_by 字段）
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
public interface ProjectService {

    /**
     * 创建项目
     * 将当前登录用户ID作为 create_by 存入数据库，实现数据隔离
     *
     * @param dto 项目新增数据传输对象
     * @return 创建结果，成功时返回项目ID
     */
    Result<Long> add(ProjectAddDTO dto);

    /**
     * 根据ID查询项目详情
     * 只能查询到当前登录用户自己创建的项目
     *
     * @param id 项目ID
     * @return 查询结果，成功时返回项目实体
     */
    Result<Project> getById(Long id);

    /**
     * 查询当前登录用户的项目列表
     * 基于 create_by 字段进行数据隔离，只返回当前用户创建的项目
     *
     * @return 查询结果，成功时返回项目列表
     */
    Result<List<Project>> listProjects();

    /**
     * 更新项目信息
     * 只能更新当前登录用户自己创建的项目
     *
     * @param dto 项目更新数据传输对象
     * @return 更新结果
     */
    Result<Void> update(ProjectUpdateDTO dto);

    /**
     * 删除项目
     * 只能删除当前登录用户自己创建的项目，执行逻辑删除
     *
     * @param id 项目ID
     * @return 删除结果
     */
    Result<Void> delete(Long id);
}
