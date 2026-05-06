package com.testplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ProjectAddDTO;
import com.testplatform.entity.dto.ProjectUpdateDTO;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目业务实现类
 * 实现项目相关的业务逻辑，包含严格的 create_by 数据隔离
 * 所有查询和修改操作均会校验当前登录用户是否为项目创建者
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 创建项目
     * 1. 从 ThreadLocal 获取当前登录用户ID，作为 create_by
     * 2. 校验同一用户下是否已存在同名项目
     * 3. 保存项目信息到数据库
     *
     * @param dto 项目新增数据传输对象
     * @return 创建结果，成功时返回项目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(ProjectAddDTO dto) {
        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录，无法创建项目");
        }

        // 参数校验：项目名称不能为空
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            return Result.error("项目名称不能为空");
        }

        // 校验同一用户下是否已存在同名项目
        if (projectMapper.existsByNameAndCreateBy(dto.getName().trim(), currentUserId)) {
            return Result.error("项目名称已存在，请更换其他名称");
        }

        // 构建项目实体对象
        Project project = Project.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .createBy(currentUserId)  // 设置创建人，实现数据隔离
                .build();

        // 插入数据库（create_time 和 update_time 由 MyMetaObjectHandler 自动填充）
        int result = projectMapper.insert(project);
        if (result != 1) {
            log.error("项目创建失败，数据库插入失败，用户ID：{}，项目名称：{}", currentUserId, dto.getName());
            return Result.error("项目创建失败，请稍后重试");
        }

        log.info("项目创建成功，项目ID：{}，项目名称：{}，创建人：{}", project.getId(), project.getName(), currentUserId);
        return Result.success("项目创建成功", project.getId());
    }

    /**
     * 根据ID查询项目详情
     * 只能查询当前登录用户自己创建的项目，通过 create_by 进行数据隔离
     *
     * @param id 项目ID
     * @return 查询结果，成功时返回项目实体
     */
    @Override
    public Result<Project> getById(Long id) {
        if (id == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 通过 ID 和 create_by 联合查询，确保数据隔离
        Project project = projectMapper.selectByIdAndCreateBy(id, currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限访问");
        }

        return Result.success(project);
    }

    /**
     * 查询当前登录用户的项目列表
     * 基于 create_by 字段进行严格的数据隔离，只返回当前用户创建的项目
     * 按 update_time 降序排列，最近更新的项目排在前面
     *
     * @return 查询结果，成功时返回项目列表
     */
    @Override
    public Result<List<Project>> listProjects() {
        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 查询当前用户创建的所有项目（已过滤逻辑删除的数据）
        List<Project> projects = projectMapper.selectListByCreateBy(currentUserId);
        return Result.success(projects);
    }

    /**
     * 更新项目信息
     * 1. 校验当前登录用户是否为该项目的创建者
     * 2. 校验更新后的项目名称是否与其他项目冲突
     * 3. 执行更新操作
     *
     * @param dto 项目更新数据传输对象
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(ProjectUpdateDTO dto) {
        if (dto == null || dto.getId() == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 校验项目是否存在且属于当前用户（数据隔离校验）
        Project existingProject = projectMapper.selectByIdAndCreateBy(dto.getId(), currentUserId);
        if (existingProject == null) {
            return Result.error("项目不存在或无权限修改");
        }

        // 如果修改了项目名称，需校验新名称是否与其他项目冲突
        if (StringUtils.hasText(dto.getName()) && !dto.getName().trim().equals(existingProject.getName())) {
            if (projectMapper.existsByNameAndCreateBy(dto.getName().trim(), currentUserId)) {
                return Result.error("项目名称已存在，请更换其他名称");
            }
        }

        // 构建更新后的项目实体
        Project updateProject = Project.builder()
                .id(dto.getId())
                .name(dto.getName() != null ? dto.getName().trim() : existingProject.getName())
                .description(dto.getDescription())
                .build();

        // 执行更新（update_time 由 MyMetaObjectHandler 自动填充）
        int result = projectMapper.updateById(updateProject);
        if (result != 1) {
            log.error("项目更新失败，项目ID：{}，用户ID：{}", dto.getId(), currentUserId);
            return Result.error("项目更新失败，请稍后重试");
        }

        log.info("项目更新成功，项目ID：{}，用户ID：{}", dto.getId(), currentUserId);
        return Result.success("项目更新成功", null);
    }

    /**
     * 删除项目
     * 1. 校验当前登录用户是否为该项目的创建者
     * 2. 执行逻辑删除（由 @TableLogic 自动处理，将 is_deleted 置为 1）
     *
     * @param id 项目ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.error("项目ID不能为空");
        }

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 校验项目是否存在且属于当前用户（数据隔离校验）
        Project existingProject = projectMapper.selectByIdAndCreateBy(id, currentUserId);
        if (existingProject == null) {
            return Result.error("项目不存在或无权限删除");
        }

        // 执行逻辑删除（MyBatis-Plus 检测到 @TableLogic 后会执行 UPDATE 而非 DELETE）
        int result = projectMapper.deleteById(id);
        if (result != 1) {
            log.error("项目删除失败，项目ID：{}，用户ID：{}", id, currentUserId);
            return Result.error("项目删除失败，请稍后重试");
        }

        log.info("项目删除成功，项目ID：{}，用户ID：{}", id, currentUserId);
        return Result.success("项目删除成功", null);
    }
}
