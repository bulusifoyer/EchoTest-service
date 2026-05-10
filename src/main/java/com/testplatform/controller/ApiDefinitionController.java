package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.dto.ApiDefinitionAddDTO;
import com.testplatform.entity.dto.ApiDefinitionUpdateDTO;
import com.testplatform.service.ApiDefinitionService;
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
 * 接口定义控制器
 * 处理接口资产相关的 HTTP 请求，提供增删改查 API
 * 所有接口均需登录（由 JwtAuthenticationInterceptor 拦截校验）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/api/apis")
@Api(tags = "接口管理接口")
@Validated
public class ApiDefinitionController {

    @Autowired
    private ApiDefinitionService apiDefinitionService;

    @PostMapping("/add")
    @ApiOperation(value = "创建接口", notes = "在指定项目下新增一条接口定义")
    public Result<Long> add(
            @Valid @RequestBody
            @ApiParam(name = "接口信息", value = "接口新增所需信息", required = true)
            ApiDefinitionAddDTO dto
    ) {
        log.info("收到创建接口请求，projectId：{}，name：{}", dto.getProjectId(), dto.getName());
        return apiDefinitionService.add(dto);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "查询接口详情", notes = "根据ID查询接口定义")
    public Result<ApiDefinition> getById(
            @PathVariable
            @NotNull(message = "接口ID不能为空")
            @ApiParam(name = "id", value = "接口ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到查询接口详情请求，id：{}", id);
        return apiDefinitionService.getById(id);
    }

    @GetMapping("/list/{projectId}")
    @ApiOperation(value = "查询项目接口列表", notes = "获取指定项目下当前用户的所有接口")
    public Result<List<ApiDefinition>> listByProjectId(
            @PathVariable
            @NotNull(message = "项目ID不能为空")
            @ApiParam(name = "projectId", value = "项目ID", required = true, example = "1")
            Long projectId
    ) {
        log.info("收到查询项目接口列表请求，projectId：{}", projectId);
        return apiDefinitionService.listByProjectId(projectId);
    }

    @PutMapping("/update")
    @ApiOperation(value = "更新接口", notes = "修改接口定义信息")
    public Result<Void> update(
            @Valid @RequestBody
            @ApiParam(name = "接口信息", value = "接口更新所需信息", required = true)
            ApiDefinitionUpdateDTO dto
    ) {
        log.info("收到更新接口请求，id：{}，name：{}", dto.getId(), dto.getName());
        return apiDefinitionService.update(dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除接口", notes = "逻辑删除接口定义")
    public Result<Void> delete(
            @PathVariable
            @NotNull(message = "接口ID不能为空")
            @ApiParam(name = "id", value = "接口ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到删除接口请求，id：{}", id);
        return apiDefinitionService.delete(id);
    }
}
