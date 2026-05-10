package com.testplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.testplatform.common.JsonUtils;
import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ApiDefinitionAddDTO;
import com.testplatform.entity.dto.ApiDefinitionUpdateDTO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.ApiDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 接口定义业务实现类
 * 实现接口资产相关的业务逻辑，包含双层数据隔离：
 *   1. 通过 t_project.create_by 校验项目归属；
 *   2. 通过 t_api_definition.create_by 冗余字段加速接口查询。
 * 骨架参照 ProjectServiceImpl / EnvironmentServiceImpl。
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@Service
public class ApiDefinitionServiceImpl extends ServiceImpl<ApiDefinitionMapper, ApiDefinition>
        implements ApiDefinitionService {

    @Autowired
    private ApiDefinitionMapper apiDefinitionMapper;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 创建接口定义
     * 1. 取当前登录用户ID（UserContext）
     * 2. 校验项目归属
     * 3. 校验 (method, path) 在该项目下唯一
     * 4. 若 requestHeaders 非空，校验合法 JSON 对象
     * 5. 插入数据库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(ApiDefinitionAddDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }
        if (dto == null || dto.getProjectId() == null) {
            return Result.error("项目ID不能为空");
        }

        // 校验项目归属
        Project project = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限操作");
        }

        // 校验 (method, path) 同项目唯一
        if (apiDefinitionMapper.existsByProjectMethodPath(
                dto.getProjectId(), dto.getMethod(), dto.getPath())) {
            return Result.error("同项目下已存在相同 method + path 的接口，请更换其他组合");
        }

        // 校验 requestHeaders JSON
        if (StringUtils.hasText(dto.getRequestHeaders())
                && !JsonUtils.isValidJsonObject(dto.getRequestHeaders())) {
            return Result.error("请求头必须是合法的 JSON 对象，例如：{\"Content-Type\":\"application/json\"}");
        }

        ApiDefinition entity = ApiDefinition.builder()
                .projectId(dto.getProjectId())
                .createBy(currentUserId)
                .name(dto.getName().trim())
                .method(dto.getMethod().trim().toUpperCase())
                .path(dto.getPath().trim())
                .requestHeaders(dto.getRequestHeaders())
                .requestBody(dto.getRequestBody())
                .build();

        int rows = apiDefinitionMapper.insert(entity);
        if (rows != 1) {
            log.error("接口定义创建失败，用户ID：{}，projectId：{}，name：{}",
                    currentUserId, dto.getProjectId(), dto.getName());
            return Result.error("接口定义创建失败，请稍后重试");
        }

        log.info("接口定义创建成功，id：{}，projectId：{}，method+path：{} {}",
                entity.getId(), entity.getProjectId(), entity.getMethod(), entity.getPath());
        return Result.success("接口创建成功", entity.getId());
    }

    /**
     * 根据ID查询接口定义详情（带归属校验）
     */
    @Override
    public Result<ApiDefinition> getById(Long id) {
        if (id == null) {
            return Result.error("接口ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        ApiDefinition entity = apiDefinitionMapper.selectByIdAndCreateBy(id, currentUserId);
        if (entity == null) {
            return Result.error("接口不存在或无权限访问");
        }
        return Result.success(entity);
    }

    /**
     * 查询项目下接口列表（带项目归属校验）
     */
    @Override
    public Result<List<ApiDefinition>> listByProjectId(Long projectId) {
        if (projectId == null) {
            return Result.error("项目ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        Project project = projectMapper.selectByIdAndCreateBy(projectId, currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限访问");
        }

        List<ApiDefinition> list = apiDefinitionMapper.selectListByProjectAndCreateBy(projectId, currentUserId);
        return Result.success(list);
    }

    /**
     * 更新接口定义
     * 1. 校验接口归属
     * 2. 如果改了 method/path，校验新组合在同项目下不重复（排除自身）
     * 3. headers JSON 校验
     * 4. 执行更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(ApiDefinitionUpdateDTO dto) {
        if (dto == null || dto.getId() == null) {
            return Result.error("接口ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 接口归属校验
        ApiDefinition existing = apiDefinitionMapper.selectByIdAndCreateBy(dto.getId(), currentUserId);
        if (existing == null) {
            return Result.error("接口不存在或无权限修改");
        }

        // 若切换了 projectId，校验新项目归属
        if (!dto.getProjectId().equals(existing.getProjectId())) {
            Project newProject = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
            if (newProject == null) {
                return Result.error("目标项目不存在或无权限操作");
            }
        }

        // method/path 变更时做唯一性校验
        String newMethod = dto.getMethod().trim().toUpperCase();
        String newPath = dto.getPath().trim();
        boolean methodOrPathChanged = !newMethod.equals(existing.getMethod())
                || !newPath.equals(existing.getPath())
                || !dto.getProjectId().equals(existing.getProjectId());
        if (methodOrPathChanged
                && apiDefinitionMapper.existsByProjectMethodPath(dto.getProjectId(), newMethod, newPath)) {
            return Result.error("同项目下已存在相同 method + path 的接口，请更换其他组合");
        }

        if (StringUtils.hasText(dto.getRequestHeaders())
                && !JsonUtils.isValidJsonObject(dto.getRequestHeaders())) {
            return Result.error("请求头必须是合法的 JSON 对象");
        }

        ApiDefinition update = ApiDefinition.builder()
                .id(dto.getId())
                .projectId(dto.getProjectId())
                .name(dto.getName().trim())
                .method(newMethod)
                .path(newPath)
                .requestHeaders(dto.getRequestHeaders())
                .requestBody(dto.getRequestBody())
                .build();

        int rows = apiDefinitionMapper.updateById(update);
        if (rows != 1) {
            log.error("接口更新失败，id：{}，userId：{}", dto.getId(), currentUserId);
            return Result.error("接口更新失败，请稍后重试");
        }

        log.info("接口更新成功，id：{}，userId：{}", dto.getId(), currentUserId);
        return Result.success("接口更新成功", null);
    }

    /**
     * 删除接口定义（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.error("接口ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        ApiDefinition existing = apiDefinitionMapper.selectByIdAndCreateBy(id, currentUserId);
        if (existing == null) {
            return Result.error("接口不存在或无权限删除");
        }

        int rows = apiDefinitionMapper.deleteById(id);
        if (rows != 1) {
            log.error("接口删除失败，id：{}，userId：{}", id, currentUserId);
            return Result.error("接口删除失败，请稍后重试");
        }

        log.info("接口删除成功，id：{}，userId：{}", id, currentUserId);
        return Result.success("接口删除成功", null);
    }
}
