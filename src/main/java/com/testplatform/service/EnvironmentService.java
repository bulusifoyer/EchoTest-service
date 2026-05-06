package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.Environment;
import com.testplatform.entity.dto.EnvironmentAddDTO;
import com.testplatform.entity.dto.EnvironmentUpdateDTO;

import java.util.List;

/**
 * 环境配置业务接口
 * 定义环境配置相关的业务逻辑规范，包括环境的增删改查
 * 所有操作需校验环境所属项目的归属权（通过 project_id 关联）
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
public interface EnvironmentService {

    /**
     * 为指定项目创建环境配置
     * 需先校验项目是否属于当前登录用户
     *
     * @param dto 环境配置新增数据传输对象
     * @return 创建结果，成功时返回环境配置ID
     */
    Result<Long> add(EnvironmentAddDTO dto);

    /**
     * 根据ID查询环境配置详情
     * 需校验环境所属项目是否属于当前登录用户
     *
     * @param id 环境配置ID
     * @return 查询结果，成功时返回环境配置实体
     */
    Result<Environment> getById(Long id);

    /**
     * 查询指定项目下的所有环境配置
     * 需先校验项目是否属于当前登录用户
     *
     * @param projectId 项目ID
     * @return 查询结果，成功时返回环境配置列表
     */
    Result<List<Environment>> listByProjectId(Long projectId);

    /**
     * 更新环境配置信息
     * 需校验环境所属项目是否属于当前登录用户
     *
     * @param dto 环境配置更新数据传输对象
     * @return 更新结果
     */
    Result<Void> update(EnvironmentUpdateDTO dto);

    /**
     * 删除环境配置
     * 需校验环境所属项目是否属于当前登录用户
     *
     * @param id 环境配置ID
     * @return 删除结果
     */
    Result<Void> delete(Long id);
}
