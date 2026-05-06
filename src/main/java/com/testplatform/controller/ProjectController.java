package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ProjectAddDTO;
import com.testplatform.entity.dto.ProjectUpdateDTO;
import com.testplatform.service.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 项目控制器
 * 处理项目相关的 HTTP 请求，提供项目的增删改查 RESTful API
 * 所有接口均需登录后才能访问（由 JwtAuthenticationInterceptor 拦截校验）
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
@Api(tags = "项目管理接口")
@Validated
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 创建项目接口
     * 接收前端的项目创建请求，将当前登录用户作为创建人存入数据库
     *
     * @param dto 项目新增信息
     * @return 创建结果，包含新项目的ID
     */
    @PostMapping("/add")
    @ApiOperation(value = "创建项目", notes = "新建一个测试项目，当前登录用户即为项目创建人")
    public Result<Long> add(
            @Valid
            @RequestBody
            @ApiParam(name = "项目信息", value = "项目新增所需信息", required = true)
            ProjectAddDTO dto
    ) {
        log.info("收到创建项目请求，项目名称：{}", dto.getName());
        return projectService.add(dto);
    }

    /**
     * 查询项目详情接口
     * 根据项目ID查询详情，只能查询到当前登录用户自己创建的项目
     *
     * @param id 项目ID
     * @return 项目详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询项目详情", notes = "根据ID查询项目详细信息")
    public Result<Project> getById(
            @PathVariable
            @NotNull(message = "项目ID不能为空")
            @ApiParam(name = "id", value = "项目ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到查询项目详情请求，项目ID：{}", id);
        return projectService.getById(id);
    }

    /**
     * 查询项目列表接口
     * 返回当前登录用户创建的所有项目列表
     *
     * @return 项目列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询项目列表", notes = "获取当前登录用户的所有项目列表")
    public Result<List<Project>> listProjects() {
        log.info("收到查询项目列表请求");
        return projectService.listProjects();
    }

    /**
     * 更新项目接口
     * 修改项目信息，只能修改当前登录用户自己创建的项目
     *
     * @param dto 项目更新信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新项目", notes = "修改项目的基础信息")
    public Result<Void> update(
            @Valid
            @RequestBody
            @ApiParam(name = "项目信息", value = "项目更新所需信息", required = true)
            ProjectUpdateDTO dto
    ) {
        log.info("收到更新项目请求，项目ID：{}，项目名称：{}", dto.getId(), dto.getName());
        return projectService.update(dto);
    }

    /**
     * 删除项目接口
     * 删除指定项目，只能删除当前登录用户自己创建的项目，执行逻辑删除
     *
     * @param id 项目ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除项目", notes = "根据ID删除项目（逻辑删除）")
    public Result<Void> delete(
            @PathVariable
            @NotNull(message = "项目ID不能为空")
            @ApiParam(name = "id", value = "项目ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到删除项目请求，项目ID：{}", id);
        return projectService.delete(id);
    }
}
