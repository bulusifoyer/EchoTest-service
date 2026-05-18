package com.testplatform.service.impl;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Environment;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ApiTryRunDTO;
import com.testplatform.entity.dto.ApiTryRunResultVO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.EnvironmentMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.ApiTryRunService;
import com.testplatform.service.execution.HeaderMerger;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 接口试调业务实现类
 * 基于 OkHttp 发起真实 HTTP 调用。不做连接池集中管控，每次试调新建 OkHttpClient。
 *
 * headers 合并优先级（低 → 高）：
 *   environment.globalHeaders → apiDefinition.requestHeaders → dto.overrideHeaders
 * 高优先级相同 key 覆盖低优先级。
 * body 非空且未显式设置 Content-Type（大小写不敏感）时，自动补 application/json;charset=UTF-8。
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@Service
public class ApiTryRunServiceImpl implements ApiTryRunService {

    @Autowired
    private ApiDefinitionMapper apiDefinitionMapper;

    @Autowired
    private EnvironmentMapper environmentMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public Result<ApiTryRunResultVO> tryRun(ApiTryRunDTO dto) {
        if (dto == null || dto.getApiId() == null || dto.getEnvId() == null) {
            return Result.error("接口ID与环境ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 1. 接口归属校验
        ApiDefinition api = apiDefinitionMapper.selectByIdAndCreateBy(dto.getApiId(), currentUserId);
        if (api == null) {
            return Result.error("接口不存在或无权限访问");
        }

        // 2. 环境归属校验（环境 → 项目 → 用户）
        Environment env = environmentMapper.selectById(dto.getEnvId());
        if (env == null) {
            return Result.error("环境不存在");
        }
        Project envProject = projectMapper.selectByIdAndCreateBy(env.getProjectId(), currentUserId);
        if (envProject == null) {
            return Result.error("环境所属项目无权限访问");
        }

        // 3. 合并 headers：env.global → api.requestHeaders → dto.overrideHeaders（后者覆盖前者）
        Map<String, String> finalHeaders;
        try {
            finalHeaders = HeaderMerger.mergeJsonHeaders(
                    env.getGlobalHeaders(),
                    api.getRequestHeaders(),
                    dto.getOverrideHeaders());
        } catch (IOException e) {
            return Result.error("请求头 JSON 非法：" + e.getMessage());
        }

        // 4. 决定最终 body：override 优先，否则使用接口定义的默认 body
        String finalBody = StringUtils.hasText(dto.getOverrideBody())
                ? dto.getOverrideBody()
                : api.getRequestBody();

        // 5. body 非空且无 Content-Type 时自动补 application/json
        if (StringUtils.hasText(finalBody) && !HeaderMerger.containsHeaderIgnoreCase(finalHeaders, "Content-Type")) {
            finalHeaders.put("Content-Type", "application/json;charset=UTF-8");
        }

        // 6. 拼最终 URL：去掉 baseUrl 末尾 / 后直接拼接 path
        String baseUrl = env.getBaseUrl() == null ? "" : env.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String finalUrl = baseUrl + api.getPath();

        // 7. 发起 OkHttp 调用
        int timeoutMs = dto.getTimeoutMs() == null ? 10000 : dto.getTimeoutMs();
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request.Builder reqBuilder = new Request.Builder().url(finalUrl);
        // 设置 headers
        for (Map.Entry<String, String> e : finalHeaders.entrySet()) {
            reqBuilder.header(e.getKey(), e.getValue());
        }
        // 根据 method 决定是否带 body
        RequestBody requestBody = null;
        String method = api.getMethod() == null ? "GET" : api.getMethod().toUpperCase();
        if (StringUtils.hasText(finalBody)) {
            // 用 Content-Type 构造 MediaType，否则默认 application/octet-stream
            String ct = HeaderMerger.getHeaderIgnoreCase(finalHeaders, "Content-Type");
            MediaType mediaType = ct != null ? MediaType.parse(ct) : null;
            requestBody = RequestBody.create(finalBody, mediaType);
        }
        try {
            reqBuilder.method(method, requestBody);
        } catch (IllegalArgumentException ex) {
            // OkHttp 对 GET 带 body 会抛异常等情况，统一兜底
            return Result.success(ApiTryRunResultVO.builder()
                    .statusCode(0).elapsedMs(0L)
                    .success(false)
                    .errorMessage("构造请求失败：" + ex.getMessage())
                    .build());
        }

        long start = System.currentTimeMillis();
        try (Response response = client.newCall(reqBuilder.build()).execute()) {
            long elapsed = System.currentTimeMillis() - start;
            Map<String, String> respHeaders = new LinkedHashMap<>();
            for (String name : response.headers().names()) {
                respHeaders.put(name, response.header(name));
            }
            String respBody = response.body() != null ? response.body().string() : "";
            log.info("接口试调成功，apiId：{}，envId：{}，status：{}，elapsed：{}ms",
                    dto.getApiId(), dto.getEnvId(), response.code(), elapsed);
            return Result.success(ApiTryRunResultVO.builder()
                    .statusCode(response.code())
                    .elapsedMs(elapsed)
                    .responseHeaders(respHeaders)
                    .responseBody(respBody)
                    .success(true)
                    .errorMessage(null)
                    .build());
        } catch (IOException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("接口试调网络异常，apiId：{}，envId：{}，elapsed：{}ms，err：{}",
                    dto.getApiId(), dto.getEnvId(), elapsed, ex.getMessage());
            return Result.success(ApiTryRunResultVO.builder()
                    .statusCode(0)
                    .elapsedMs(elapsed)
                    .success(false)
                    .errorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                    .build());
        }
    }

    /**
     * 请求头合并/查找/移除等工具已抽取到 {@link com.testplatform.service.execution.HeaderMerger}，
     * 以便 M3 执行引擎与单接口试调共用，避免两边算法分裂。
     */
}
