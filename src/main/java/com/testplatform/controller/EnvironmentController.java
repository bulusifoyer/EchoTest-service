package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.Environment;
import com.testplatform.entity.dto.EnvironmentAddDTO;
import com.testplatform.entity.dto.EnvironmentUpdateDTO;
import com.testplatform.service.EnvironmentService;
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
 * 环境配置控制器
 * 处理环境配置相关的 HTTP 请求，提供环境配置的增删改查 RESTful API
 * 所有接口均需登录后才能访问，且会校验环境所属项目的归属权
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@RestController
@RequestMapping("/api/environments")
@Api(tags = "环境配置接口")
@Validated
public class EnvironmentController {

    @Autowired
    private EnvironmentService environmentService;

    /**
     * 创建环境配置接口
     * 为指定项目添加环境配置，需校验项目是否属于当前登录用户
     *
     * @param dto 环境配置新增信息
     * @return 创建结果，包含新环境配置的ID
     */
    @PostMapping("/add")
    @ApiOperation(value = "创建环境配置", notes = "为指定项目添加环境配置（如开发环境、测试环境）")
    public Result<Long> add(
            @Valid
            @RequestBody
            @ApiParam(name = "环境配置信息", value = "环境配置新增所需信息", required = true)
            EnvironmentAddDTO dto
    ) {
        log.info("收到创建环境配置请求，项目ID：{}，环境名称：{}", dto.getProjectId(), dto.getEnvName());
        return environmentService.add(dto);
    }

    /**
     * 查询环境配置详情接口
     * 根据环境配置ID查询详情，会校验环境所属项目的归属权
     *
     * @param id 环境配置ID
     * @return 环境配置详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询环境配置详情", notes = "根据ID查询环境配置详细信息")
    public Result<Environment> getById(
            @PathVariable
            @NotNull(message = "环境配置ID不能为空")
            @ApiParam(name = "id", value = "环境配置ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到查询环境配置详情请求，环境配置ID：{}", id);
        return environmentService.getById(id);
    }

    /**
     * 查询项目下的环境配置列表接口
     * 获取指定项目下的所有环境配置，需校验项目是否属于当前登录用户
     *
     * @param projectId 项目ID
     * @return 环境配置列表
     */
    @GetMapping("/list/{projectId}")
    @ApiOperation(value = "查询项目环境列表", notes = "获取指定项目下的所有环境配置列表")
    public Result<List<Environment>> listByProjectId(
            @PathVariable
            @NotNull(message = "项目ID不能为空")
            @ApiParam(name = "projectId", value = "项目ID", required = true, example = "1")
            Long projectId
    ) {
        log.info("收到查询项目环境列表请求，项目ID：{}", projectId);
        return environmentService.listByProjectId(projectId);
    }

    /**
     * 更新环境配置接口
     * 修改环境配置信息，会校验环境所属项目的归属权
     *
     * @param dto 环境配置更新信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新环境配置", notes = "修改环境配置的基础信息")
    public Result<Void> update(
            @Valid
            @RequestBody
            @ApiParam(name = "环境配置信息", value = "环境配置更新所需信息", required = true)
            EnvironmentUpdateDTO dto
    ) {
        log.info("收到更新环境配置请求，环境配置ID：{}，环境名称：{}", dto.getId(), dto.getEnvName());
        return environmentService.update(dto);
    }

    /**
     * 删除环境配置接口
     * 删除指定环境配置，会校验环境所属项目的归属权
     *
     * @param id 环境配置ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除环境配置", notes = "根据ID删除环境配置")
    public Result<Void> delete(
            @PathVariable
            @NotNull(message = "环境配置ID不能为空")
            @ApiParam(name = "id", value = "环境配置ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到删除环境配置请求，环境配置ID：{}", id);
        return environmentService.delete(id);
    }
}
