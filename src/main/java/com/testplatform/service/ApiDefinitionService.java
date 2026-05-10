package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.dto.ApiDefinitionAddDTO;
import com.testplatform.entity.dto.ApiDefinitionUpdateDTO;

import java.util.List;

/**
 * 接口定义业务接口
 * 定义接口资产相关的业务逻辑规范，包括接口的增删改查
 * 所有操作均进行数据隔离（基于 create_by 字段）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
public interface ApiDefinitionService {

    /**
     * 创建接口定义
     * 将当前登录用户ID作为 create_by 存入数据库，实现数据隔离
     * 同时校验：项目归属、同项目 (method, path) 唯一性、headers 合法 JSON
     *
     * @param dto 接口新增数据传输对象
     * @return 创建结果，成功时返回接口ID
     */
    Result<Long> add(ApiDefinitionAddDTO dto);

    /**
     * 根据ID查询接口定义详情
     * 只能查询到当前登录用户自己创建的接口
     *
     * @param id 接口ID
     * @return 查询结果，成功时返回接口定义实体
     */
    Result<ApiDefinition> getById(Long id);

    /**
     * 查询指定项目下当前用户的所有接口定义
     * 会先校验项目归属
     *
     * @param projectId 项目ID
     * @return 接口定义列表
     */
    Result<List<ApiDefinition>> listByProjectId(Long projectId);

    /**
     * 更新接口定义
     * 只能更新当前登录用户自己创建的接口
     *
     * @param dto 接口更新数据传输对象
     * @return 更新结果
     */
    Result<Void> update(ApiDefinitionUpdateDTO dto);

    /**
     * 删除接口定义
     * 只能删除当前登录用户自己创建的接口，执行逻辑删除
     *
     * @param id 接口ID
     * @return 删除结果
     */
    Result<Void> delete(Long id);
}
