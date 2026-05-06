package com.testplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.testplatform.common.JsonUtils;
import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.Environment;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.EnvironmentAddDTO;
import com.testplatform.entity.dto.EnvironmentUpdateDTO;
import com.testplatform.mapper.EnvironmentMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.EnvironmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 环境配置业务实现类
 * 实现环境配置的增删改查，所有操作需先校验所属项目的归属权
 * 通过 project_id 关联项目表，确保用户只能操作自己项目下的环境配置
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Service
public class EnvironmentServiceImpl extends ServiceImpl<EnvironmentMapper, Environment> implements EnvironmentService {

    @Autowired
    private EnvironmentMapper environmentMapper;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 创建环境配置
     * 1. 校验当前登录用户是否为该项目的创建者
     * 2. 校验同一项目下是否已存在同名环境
     * 3. 保存环境配置到数据库
     *
     * @param dto 环境配置新增数据传输对象
     * @return 创建结果，成功时返回环境配置ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(EnvironmentAddDTO dto) {
        if (dto == null || dto.getProjectId() == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 校验项目是否存在且属于当前用户（归属权校验）
        Project project = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限操作");
        }

        // 校验环境名称是否为空
        if (!StringUtils.hasText(dto.getEnvName())) {
            return Result.error("环境名称不能为空");
        }

        // 校验同一项目下是否已存在同名环境
        Environment existingEnv = environmentMapper.selectByEnvNameAndProjectId(
                dto.getEnvName().trim(), dto.getProjectId());
        if (existingEnv != null) {
            return Result.error("该项目下已存在同名环境，请更换其他名称");
        }

        // 校验 global_headers 是否为合法的 JSON 格式（MVP 版本要求）
        if (StringUtils.hasText(dto.getGlobalHeaders())) {
            if (!JsonUtils.isValidJsonObject(dto.getGlobalHeaders())) {
                return Result.error("全局请求头必须是合法的 JSON 对象格式，例如：{\"Authorization\": \"Bearer xxx\"}");
            }
        }

        // 构建环境配置实体
        Environment environment = Environment.builder()
                .projectId(dto.getProjectId())
                .envName(dto.getEnvName().trim())
                .baseUrl(dto.getBaseUrl() != null ? dto.getBaseUrl().trim() : null)
                .globalHeaders(dto.getGlobalHeaders())
                .build();

        // 插入数据库（create_time 和 update_time 由 MyMetaObjectHandler 自动填充）
        int result = environmentMapper.insert(environment);
        if (result != 1) {
            log.error("环境配置创建失败，数据库插入失败，项目ID：{}，环境名称：{}",
                    dto.getProjectId(), dto.getEnvName());
            return Result.error("环境配置创建失败，请稍后重试");
        }

        log.info("环境配置创建成功，环境ID：{}，项目ID：{}，环境名称：{}",
                environment.getId(), dto.getProjectId(), environment.getEnvName());
        return Result.success("环境配置创建成功", environment.getId());
    }

    /**
     * 根据ID查询环境配置详情
     * 需校验环境所属项目是否属于当前登录用户
     *
     * @param id 环境配置ID
     * @return 查询结果，成功时返回环境配置实体
     */
    @Override
    public Result<Environment> getById(Long id) {
        if (id == null) {
            return Result.error("环境配置ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 查询环境配置
        Environment environment = environmentMapper.selectById(id);
        if (environment == null) {
            return Result.error("环境配置不存在");
        }

        // 校验环境所属项目是否属于当前用户（归属权校验）
        Project project = projectMapper.selectByIdAndCreateBy(environment.getProjectId(), currentUserId);
        if (project == null) {
            return Result.error("环境配置不存在或无权限访问");
        }

        return Result.success(environment);
    }

    /**
     * 查询指定项目下的所有环境配置
     * 需先校验项目是否属于当前登录用户
     *
     * @param projectId 项目ID
     * @return 查询结果，成功时返回环境配置列表
     */
    @Override
    public Result<List<Environment>> listByProjectId(Long projectId) {
        if (projectId == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 校验项目是否存在且属于当前用户（归属权校验）
        Project project = projectMapper.selectByIdAndCreateBy(projectId, currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限访问");
        }

        // 查询该项目下的所有环境配置
        List<Environment> environments = environmentMapper.selectListByProjectId(projectId);
        return Result.success(environments);
    }

    /**
     * 更新环境配置信息
     * 1. 校验环境配置是否存在
     * 2. 校验环境所属项目是否属于当前登录用户
     * 3. 如果更换了项目，需校验新项目是否也属于当前用户
     * 4. 执行更新操作
     *
     * @param dto 环境配置更新数据传输对象
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(EnvironmentUpdateDTO dto) {
        if (dto == null || dto.getId() == null) {
            return Result.error("环境配置ID不能为空");
        }
        if (dto.getProjectId() == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 查询要更新的环境配置
        Environment existingEnv = environmentMapper.selectById(dto.getId());
        if (existingEnv == null) {
            return Result.error("环境配置不存在");
        }

        // 校验原项目是否属于当前用户（归属权校验）
        Project originalProject = projectMapper.selectByIdAndCreateBy(existingEnv.getProjectId(), currentUserId);
        if (originalProject == null) {
            return Result.error("环境配置不存在或无权限修改");
        }

        // 如果项目ID发生变更，需校验新项目是否也属于当前用户
        if (!dto.getProjectId().equals(existingEnv.getProjectId())) {
            Project newProject = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
            if (newProject == null) {
                return Result.error("目标项目不存在或无权限操作");
            }
        }

        // 如果修改了环境名称，需校验新名称是否在同一项目下冲突
        if (StringUtils.hasText(dto.getEnvName())
                && !dto.getEnvName().trim().equals(existingEnv.getEnvName())) {
            Environment nameConflict = environmentMapper.selectByEnvNameAndProjectId(
                    dto.getEnvName().trim(), dto.getProjectId());
            if (nameConflict != null && !nameConflict.getId().equals(dto.getId())) {
                return Result.error("该项目下已存在同名环境，请更换其他名称");
            }
        }

        // 校验 global_headers 是否为合法的 JSON 格式（MVP 版本要求）
        if (StringUtils.hasText(dto.getGlobalHeaders())) {
            if (!JsonUtils.isValidJsonObject(dto.getGlobalHeaders())) {
                return Result.error("全局请求头必须是合法的 JSON 对象格式，例如：{\"Authorization\": \"Bearer xxx\"}");
            }
        }

        // 构建更新后的环境配置实体
        Environment updateEnv = Environment.builder()
                .id(dto.getId())
                .projectId(dto.getProjectId())
                .envName(dto.getEnvName() != null ? dto.getEnvName().trim() : existingEnv.getEnvName())
                .baseUrl(dto.getBaseUrl() != null ? dto.getBaseUrl().trim() : existingEnv.getBaseUrl())
                .globalHeaders(dto.getGlobalHeaders())
                .build();

        // 执行更新（update_time 由 MyMetaObjectHandler 自动填充）
        int result = environmentMapper.updateById(updateEnv);
        if (result != 1) {
            log.error("环境配置更新失败，环境ID：{}，用户ID：{}", dto.getId(), currentUserId);
            return Result.error("环境配置更新失败，请稍后重试");
        }

        log.info("环境配置更新成功，环境ID：{}，项目ID：{}，用户ID：{}",
                dto.getId(), dto.getProjectId(), currentUserId);
        return Result.success("环境配置更新成功", null);
    }

    /**
     * 删除环境配置
     * 1. 校验环境配置是否存在
     * 2. 校验环境所属项目是否属于当前登录用户
     * 3. 执行物理删除（环境配置表无逻辑删除字段）
     *
     * @param id 环境配置ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.error("环境配置ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 查询要删除的环境配置
        Environment environment = environmentMapper.selectById(id);
        if (environment == null) {
            return Result.error("环境配置不存在");
        }

        // 校验环境所属项目是否属于当前用户（归属权校验）
        Project project = projectMapper.selectByIdAndCreateBy(environment.getProjectId(), currentUserId);
        if (project == null) {
            return Result.error("环境配置不存在或无权限删除");
        }

        // 执行物理删除（t_environment 表无逻辑删除字段）
        int result = environmentMapper.deleteById(id);
        if (result != 1) {
            log.error("环境配置删除失败，环境ID：{}，用户ID：{}", id, currentUserId);
            return Result.error("环境配置删除失败，请稍后重试");
        }

        log.info("环境配置删除成功，环境ID：{}，项目ID：{}，用户ID：{}",
                id, environment.getProjectId(), currentUserId);
        return Result.success("环境配置删除成功", null);
    }
}
