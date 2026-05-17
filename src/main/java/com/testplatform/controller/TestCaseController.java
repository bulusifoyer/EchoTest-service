package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.dto.TestCaseDetailVO;
import com.testplatform.entity.dto.TestCaseSaveDTO;
import com.testplatform.service.TestCaseService;
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
 * 测试用例控制器
 * 一次性管理：用例 + 步骤 + 变量提取 + 断言（聚合接口）
 * 所有接口需登录（由 JwtAuthenticationInterceptor 拦截）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@Api(tags = "测试用例接口")
@Validated
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @PostMapping("/add")
    @ApiOperation(value = "创建用例", notes = "一次性提交：用例 + 所有步骤 + 每步的变量提取与断言")
    public Result<Long> add(
            @Valid @RequestBody
            @ApiParam(name = "用例信息", value = "用例聚合数据", required = true)
            TestCaseSaveDTO dto
    ) {
        log.info("收到创建用例请求，projectId：{}，caseName：{}，stepsCount：{}",
                dto.getProjectId(), dto.getCaseName(),
                dto.getSteps() == null ? 0 : dto.getSteps().size());
        return testCaseService.add(dto);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "查询用例详情", notes = "返回用例 + 完整步骤树（含变量提取与断言）")
    public Result<TestCaseDetailVO> getDetail(
            @PathVariable
            @NotNull(message = "用例ID不能为空")
            @ApiParam(name = "id", value = "用例ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到查询用例详情请求，id：{}", id);
        return testCaseService.getDetail(id);
    }

    @GetMapping("/list/{projectId}")
    @ApiOperation(value = "查询项目用例列表", notes = "轻量列表，不含步骤树")
    public Result<List<TestCase>> listByProjectId(
            @PathVariable
            @NotNull(message = "项目ID不能为空")
            @ApiParam(name = "projectId", value = "项目ID", required = true, example = "1")
            Long projectId
    ) {
        log.info("收到查询项目用例列表请求，projectId：{}", projectId);
        return testCaseService.listByProjectId(projectId);
    }

    @PutMapping("/update/{id}")
    @ApiOperation(value = "更新用例", notes = "全量替换：先删旧步骤/变量/断言再按新数据写入")
    public Result<Void> update(
            @PathVariable
            @NotNull(message = "用例ID不能为空")
            @ApiParam(name = "id", value = "用例ID", required = true, example = "1")
            Long id,
            @Valid @RequestBody
            @ApiParam(name = "用例信息", value = "用例聚合数据", required = true)
            TestCaseSaveDTO dto
    ) {
        log.info("收到更新用例请求，id：{}，stepsCount：{}",
                id, dto.getSteps() == null ? 0 : dto.getSteps().size());
        return testCaseService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除用例", notes = "用例软删；步骤/变量/断言物理删除")
    public Result<Void> delete(
            @PathVariable
            @NotNull(message = "用例ID不能为空")
            @ApiParam(name = "id", value = "用例ID", required = true, example = "1")
            Long id
    ) {
        log.info("收到删除用例请求，id：{}", id);
        return testCaseService.delete(id);
    }
}
