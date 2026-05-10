package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.ApiTryRunDTO;
import com.testplatform.entity.dto.ApiTryRunResultVO;
import com.testplatform.service.ApiTryRunService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 接口试调控制器
 * 提供在线试调 HTTP 接口的能力，基于接口定义 + 环境配置真实发起一次请求
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/api/apis")
@Api(tags = "接口试调")
@Validated
public class ApiTryRunController {

    @Autowired
    private ApiTryRunService apiTryRunService;

    @PostMapping("/try-run")
    @ApiOperation(value = "试调接口", notes = "基于接口定义 + 环境配置真实发起一次 HTTP 请求并返回响应摘要")
    public Result<ApiTryRunResultVO> tryRun(
            @Valid @RequestBody
            @ApiParam(name = "试调参数", value = "试调所需的接口ID、环境ID与可选覆盖值", required = true)
            ApiTryRunDTO dto
    ) {
        log.info("收到接口试调请求，apiId：{}，envId：{}，timeoutMs：{}",
                dto.getApiId(), dto.getEnvId(), dto.getTimeoutMs());
        return apiTryRunService.tryRun(dto);
    }
}
