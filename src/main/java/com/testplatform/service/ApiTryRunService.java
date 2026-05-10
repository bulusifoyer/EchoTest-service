package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.ApiTryRunDTO;
import com.testplatform.entity.dto.ApiTryRunResultVO;

/**
 * 接口试调业务接口
 * 基于接口定义 + 环境配置，真实发起一次 HTTP 调用并返回响应摘要
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
public interface ApiTryRunService {

    /**
     * 试调一个接口
     * 整体流程：
     * 1. 校验接口、环境分别归属当前用户；
     * 2. 合并 headers（env.globalHeaders → api.requestHeaders → dto.overrideHeaders，高覆低）；
     * 3. 非空 body 且未显式设置 Content-Type 时自动补 application/json;charset=UTF-8；
     * 4. OkHttp 发起调用，网络异常统一封装为 success=false；
     *
     * @param dto 试调参数
     * @return 试调结果
     */
    Result<ApiTryRunResultVO> tryRun(ApiTryRunDTO dto);
}
