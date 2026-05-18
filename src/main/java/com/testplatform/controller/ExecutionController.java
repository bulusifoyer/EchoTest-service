package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.ExecutionReportDetailVO;
import com.testplatform.entity.dto.ExecutionReportVO;
import com.testplatform.entity.dto.ExecutionRunDTO;
import com.testplatform.service.ExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 测试执行与报告控制器（M3）
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
@RestController
@RequestMapping("/api/executions")
@Api(tags = "测试执行与报告")
@Validated
public class ExecutionController {

    @Autowired
    private ExecutionService executionService;

    @PostMapping("/run")
    @ApiOperation(value = "执行用例", notes = "同步串行执行用例并返回报告 ID（遇错即停）")
    public Result<Long> run(
            @Valid @RequestBody
            @ApiParam(name = "执行参数", value = "用例 ID + 环境 ID + 可选超时", required = true)
            ExecutionRunDTO dto) {
        log.info("收到用例执行请求，caseId：{}，envId：{}，timeoutMs：{}",
                dto.getCaseId(), dto.getEnvId(), dto.getTimeoutMs());
        return executionService.run(dto);
    }

    @GetMapping("/{reportId}")
    @ApiOperation(value = "查询报告详情", notes = "返回报告主表 + 全部明细")
    public Result<ExecutionReportDetailVO> getDetail(
            @ApiParam(value = "报告ID", required = true) @PathVariable Long reportId) {
        return executionService.getReportDetail(reportId);
    }

    @GetMapping("/list/{projectId}")
    @ApiOperation(value = "查询项目报告列表", notes = "按创建时间倒序，仅返回当前用户的报告")
    public Result<List<ExecutionReportVO>> list(
            @ApiParam(value = "项目ID", required = true) @PathVariable Long projectId) {
        return executionService.listByProject(projectId);
    }

    @DeleteMapping("/{reportId}")
    @ApiOperation(value = "删除报告", notes = "软删报告，明细不物理删除")
    public Result<Void> delete(
            @ApiParam(value = "报告ID", required = true) @PathVariable Long reportId) {
        return executionService.deleteReport(reportId);
    }
}
