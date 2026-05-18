package com.testplatform.service.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Assertion;
import com.testplatform.entity.Environment;
import com.testplatform.entity.ExecutionDetail;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.entity.Project;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestStep;
import com.testplatform.entity.User;
import com.testplatform.entity.VariableExtract;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.AssertionMapper;
import com.testplatform.mapper.EnvironmentMapper;
import com.testplatform.mapper.ExecutionDetailMapper;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.mapper.TestStepMapper;
import com.testplatform.mapper.UserMapper;
import com.testplatform.mapper.VariableExtractMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行引擎核心
 *
 * 业务编排（同步串行 + 遇错即停）：
 *   1. 校验：caseId / envId / 用例下每步引用的 apiId 都必须属于当前用户；env 必须与 case 同项目
 *   2. 创建报告（status=RUNNING）
 *   3. 按 step_order ASC 串行执行：
 *      a. 合并 headers (env.global → api.requestHeaders)；body 走 step.overrideRequestBody 优先，否则 api.requestBody
 *      b. VariableContext.replace url / headers / body 三处
 *      c. body 非空且无 Content-Type → 自动补 application/json;charset=UTF-8
 *      d. HttpExecutor.execute
 *      e. 写一条 t_execution_detail（含实际请求/响应摘要、状态、断言结果）
 *      f. 网络异常 → status=FAILED + failReason，break
 *      g. 跑 extracts（失败不阻断，仅记录到 detail.assertResult 中的 _extracts 节点）
 *      h. 跑 assertions（任一不过 → status=FAILED + failReason，break；全过 → passed_steps++）
 *   4. 收尾：status=PASSED/FAILED，end_time=now，total_duration_ms=summing
 *
 * 数据隔离：所有业务校验都通过 *Mapper.selectByIdAndCreateBy(...) 完成，确保 SQL 层就过滤越权访问。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
@Service
public class ExecutionEngine {

    /** 报告状态 */
    public static final String REPORT_STATUS_RUNNING = "RUNNING";
    public static final String REPORT_STATUS_PASSED  = "PASSED";
    public static final String REPORT_STATUS_FAILED  = "FAILED";

    /** 单步状态 */
    public static final String STEP_STATUS_PASSED = "PASSED";
    public static final String STEP_STATUS_FAILED = "FAILED";

    @Autowired private TestCaseMapper testCaseMapper;
    @Autowired private TestStepMapper testStepMapper;
    @Autowired private VariableExtractMapper variableExtractMapper;
    @Autowired private AssertionMapper assertionMapper;
    @Autowired private ApiDefinitionMapper apiDefinitionMapper;
    @Autowired private EnvironmentMapper environmentMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private ExecutionReportMapper reportMapper;
    @Autowired private ExecutionDetailMapper detailMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private HttpExecutor httpExecutor;

    private final Asserter asserter = new Asserter();
    private final Extractor extractor = new Extractor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 准备阶段返回值（校验通过后的所有上下文）；校验失败时 errorMessage 填原因
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrepareResult {
        private boolean ok;
        private String errorMessage;
        private TestCase testCase;
        private Environment env;
        private Project project;
        private List<TestStep> steps;
        // step.id → 该步骤的接口（已校验归属）
        private Map<Long, ApiDefinition> apiByStepId;
        // step.id → 提取规则
        private Map<Long, List<VariableExtract>> extractsByStepId;
        // step.id → 断言规则
        private Map<Long, List<Assertion>> assertionsByStepId;
    }

    /**
     * 仅做归属校验与数据加载，**不写库**；用于 ExecutionService 在创建报告之前提前失败
     */
    public PrepareResult prepare(Long caseId, Long envId, Long currentUserId) {
        if (caseId == null || envId == null) {
            return PrepareResult.builder().ok(false).errorMessage("用例ID与环境ID不能为空").build();
        }
        if (currentUserId == null) {
            return PrepareResult.builder().ok(false).errorMessage("用户未登录").build();
        }

        // 1. 用例归属
        TestCase tc = testCaseMapper.selectByIdAndCreateBy(caseId, currentUserId);
        if (tc == null) {
            return PrepareResult.builder().ok(false).errorMessage("用例不存在或无权限访问").build();
        }

        // 2. 环境归属（环境 → 项目 → 用户）
        Environment env = environmentMapper.selectById(envId);
        if (env == null) {
            return PrepareResult.builder().ok(false).errorMessage("环境不存在").build();
        }
        Project envProject = projectMapper.selectByIdAndCreateBy(env.getProjectId(), currentUserId);
        if (envProject == null) {
            return PrepareResult.builder().ok(false).errorMessage("环境所属项目无权限访问").build();
        }

        // 3. env 与 case 必须同项目（约束 14）
        if (!env.getProjectId().equals(tc.getProjectId())) {
            return PrepareResult.builder().ok(false).errorMessage("环境与用例不属于同一项目").build();
        }

        // 4. 步骤
        List<TestStep> steps = testStepMapper.selectListByCaseIdOrderByStepOrder(caseId);
        if (steps == null || steps.isEmpty()) {
            return PrepareResult.builder().ok(false).errorMessage("用例下无任何步骤，无法执行").build();
        }

        // 5. 每步引用的接口必须属于当前用户 + 同项目
        Map<Long, ApiDefinition> apiByStepId = new HashMap<>();
        for (TestStep s : steps) {
            ApiDefinition api = apiDefinitionMapper.selectByIdAndCreateBy(s.getApiId(), currentUserId);
            if (api == null) {
                return PrepareResult.builder().ok(false)
                        .errorMessage("步骤引用的接口不存在或无权限：apiId=" + s.getApiId()).build();
            }
            if (!api.getProjectId().equals(tc.getProjectId())) {
                return PrepareResult.builder().ok(false)
                        .errorMessage("步骤引用的接口与用例不属于同一项目：apiId=" + s.getApiId()).build();
            }
            apiByStepId.put(s.getId(), api);
        }

        // 6. 一次性查所有 extracts / assertions
        List<Long> stepIds = steps.stream().map(TestStep::getId).collect(Collectors.toList());
        Map<Long, List<VariableExtract>> extractsByStepId = new HashMap<>();
        Map<Long, List<Assertion>> assertionsByStepId = new HashMap<>();
        if (!stepIds.isEmpty()) {
            List<VariableExtract> allExtracts = variableExtractMapper.selectListByStepIds(stepIds);
            if (allExtracts != null) {
                for (VariableExtract ve : allExtracts) {
                    extractsByStepId.computeIfAbsent(ve.getStepId(), k -> new ArrayList<>()).add(ve);
                }
            }
            List<Assertion> allAssertions = assertionMapper.selectListByStepIds(stepIds);
            if (allAssertions != null) {
                for (Assertion a : allAssertions) {
                    assertionsByStepId.computeIfAbsent(a.getStepId(), k -> new ArrayList<>()).add(a);
                }
            }
        }

        return PrepareResult.builder()
                .ok(true)
                .testCase(tc).env(env).project(envProject)
                .steps(steps)
                .apiByStepId(apiByStepId)
                .extractsByStepId(extractsByStepId)
                .assertionsByStepId(assertionsByStepId)
                .build();
    }

    /**
     * 执行入口：完成校验、写报告、串行执行、收尾。
     *
     * @return 报告 ID（执行失败时仍返回 reportId，前端跳详情查看；如果校验阶段就失败则返回 null）
     */
    public Long run(Long caseId, Long envId, Integer timeoutMs, Long currentUserId) {
        PrepareResult prep = prepare(caseId, envId, currentUserId);
        if (!prep.isOk()) {
            log.warn("M3 执行准备失败：caseId={} envId={} reason={}", caseId, envId, prep.getErrorMessage());
            // 把校验失败上抛由 Service 层处理；引擎本身不写报告
            throw new IllegalArgumentException(prep.getErrorMessage());
        }

        TestCase tc = prep.getTestCase();
        Environment env = prep.getEnv();
        List<TestStep> steps = prep.getSteps();

        int finalTimeout = (timeoutMs == null) ? 10000 : timeoutMs;
        if (finalTimeout < 100) finalTimeout = 100;
        if (finalTimeout > 60000) finalTimeout = 60000;

        // 1. 创建报告（RUNNING）
        LocalDateTime now = LocalDateTime.now();
        String executor = resolveExecutor(currentUserId);
        ExecutionReport report = ExecutionReport.builder()
                .caseId(tc.getId())
                .projectId(tc.getProjectId())
                .createBy(currentUserId)
                .envId(env.getId())
                .status(REPORT_STATUS_RUNNING)
                .totalSteps(steps.size())
                .passedSteps(0)
                .failedSteps(0)
                .startTime(now)
                .endTime(null)
                .totalDurationMs(0L)
                .executor(executor)
                .isDeleted(0)
                .build();
        reportMapper.insert(report);
        Long reportId = report.getId();
        log.info("M3 执行开始 reportId={} caseId={} envId={} totalSteps={}",
                reportId, tc.getId(), env.getId(), steps.size());

        // 2. 串行执行
        VariableContext ctx = new VariableContext();
        long totalDuration = 0L;
        int passedCount = 0;
        int failedCount = 0;
        boolean allPassed = true;

        for (TestStep step : steps) {
            ApiDefinition api = prep.getApiByStepId().get(step.getId());
            List<VariableExtract> extracts = prep.getExtractsByStepId()
                    .getOrDefault(step.getId(), Collections.emptyList());
            List<Assertion> assertions = prep.getAssertionsByStepId()
                    .getOrDefault(step.getId(), Collections.emptyList());

            StepRun result = runOneStep(reportId, step, api, env, extracts, assertions, ctx, finalTimeout);
            totalDuration += result.elapsedMs;

            if (result.passed) {
                passedCount++;
            } else {
                failedCount++;
                allPassed = false;
                // 遇错即停（约束 1）：当前步已写 detail，后续步骤不再执行
                break;
            }
        }

        // 3. 收尾
        LocalDateTime end = LocalDateTime.now();
        ExecutionReport summary = new ExecutionReport();
        summary.setId(reportId);
        summary.setStatus(allPassed ? REPORT_STATUS_PASSED : REPORT_STATUS_FAILED);
        summary.setPassedSteps(passedCount);
        summary.setFailedSteps(failedCount);
        summary.setEndTime(end);
        summary.setTotalDurationMs(totalDuration);
        reportMapper.updateById(summary);

        log.info("M3 执行结束 reportId={} status={} passed={} failed={} totalMs={}",
                reportId, summary.getStatus(), passedCount, failedCount, totalDuration);
        return reportId;
    }

    // ---------- 内部 ----------

    /**
     * 单步执行结果（仅本类内部使用）
     */
    private static class StepRun {
        boolean passed;
        long elapsedMs;
    }

    /**
     * 执行单个步骤并写 detail
     */
    private StepRun runOneStep(Long reportId, TestStep step, ApiDefinition api, Environment env,
                               List<VariableExtract> extracts, List<Assertion> assertions,
                               VariableContext ctx, int timeoutMs) {
        StepRun result = new StepRun();

        // 1. 合并原始 headers
        Map<String, String> mergedHeaders;
        try {
            mergedHeaders = HeaderMerger.mergeJsonHeaders(env.getGlobalHeaders(), api.getRequestHeaders());
        } catch (IOException ioe) {
            mergedHeaders = new LinkedHashMap<>();
            log.warn("M3 步骤 headers 合并失败 stepOrder={} err={}", step.getStepOrder(), ioe.getMessage());
        }

        // 2. 决定原始 body：step.overrideRequestBody 优先，否则 api.requestBody
        String rawBody = (step.getOverrideRequestBody() != null && !step.getOverrideRequestBody().isEmpty())
                ? step.getOverrideRequestBody()
                : api.getRequestBody();

        // 3. 拼最终 URL（baseUrl 末尾 / 处理）
        String baseUrl = env.getBaseUrl() == null ? "" : env.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String rawUrl = baseUrl + (api.getPath() == null ? "" : api.getPath());

        // 4. 变量替换：URL / headers / body 三处
        String finalUrl = ctx.replace(rawUrl);
        String finalBody = ctx.replace(rawBody);
        Map<String, String> finalHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mergedHeaders.entrySet()) {
            finalHeaders.put(ctx.replace(e.getKey()), ctx.replace(e.getValue()));
        }

        // 5. body 非空且无 Content-Type → 自动补
        if (finalBody != null && !finalBody.isEmpty()
                && !HeaderMerger.containsHeaderIgnoreCase(finalHeaders, "Content-Type")) {
            finalHeaders.put("Content-Type", "application/json;charset=UTF-8");
        }

        // 6. 发送
        HttpExecutor.ExecuteResult exec = httpExecutor.execute(
                api.getMethod(), finalUrl, finalHeaders, finalBody, timeoutMs);
        result.elapsedMs = exec.getElapsedMs();

        // 7. 提取（网络异常时跳过提取，但仍写 detail）
        List<Extractor.ExtractResult> extractResults = Collections.emptyList();
        if (exec.isSuccess()) {
            extractResults = extractor.extractAll(extracts, exec.getResponseBody(), ctx);
        }

        // 8. 断言（网络异常 → 不跑断言，整步失败）
        boolean passed;
        String failReason = null;
        List<Asserter.AssertResult> assertResults = Collections.emptyList();
        if (!exec.isSuccess()) {
            passed = false;
            failReason = "网络异常：" + exec.getErrorMessage();
        } else {
            assertResults = asserter.assertAll(assertions, exec.getStatusCode(), exec.getResponseBody());
            passed = assertResults.stream().allMatch(Asserter.AssertResult::isPassed);
            if (!passed) {
                failReason = assertResults.stream().filter(r -> !r.isPassed())
                        .findFirst()
                        .map(Asserter.AssertResult::getMessage)
                        .orElse("断言不通过");
                failReason = "断言不通过：" + failReason;
            }
        }
        result.passed = passed;

        // 9. 写 detail
        ExecutionDetail detail = ExecutionDetail.builder()
                .reportId(reportId)
                .stepId(step.getId())
                .stepOrder(step.getStepOrder())
                .requestMethod(api.getMethod())
                .requestUrl(finalUrl)
                .actualRequest(toJsonSafe(buildRequestSnapshot(api.getMethod(), finalUrl, finalHeaders, finalBody)))
                .actualResponse(toJsonSafe(buildResponseSnapshot(exec)))
                .statusCode(exec.getStatusCode())
                .assertResult(toJsonSafe(buildAssertSnapshot(assertResults, extractResults)))
                .failReason(failReason)
                .status(passed ? STEP_STATUS_PASSED : STEP_STATUS_FAILED)
                .elapsedMs(exec.getElapsedMs())
                .build();
        detailMapper.insert(detail);

        return result;
    }

    private Map<String, Object> buildRequestSnapshot(String method, String url,
                                                     Map<String, String> headers, String body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", method);
        m.put("url", url);
        m.put("headers", headers);
        m.put("body", body);
        return m;
    }

    private Map<String, Object> buildResponseSnapshot(HttpExecutor.ExecuteResult exec) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("statusCode", exec.getStatusCode());
        m.put("headers", exec.getResponseHeaders());
        m.put("body", exec.getResponseBody());
        m.put("elapsedMs", exec.getElapsedMs());
        m.put("success", exec.isSuccess());
        if (!exec.isSuccess()) {
            m.put("errorMessage", exec.getErrorMessage());
        }
        return m;
    }

    private Map<String, Object> buildAssertSnapshot(List<Asserter.AssertResult> assertResults,
                                                    List<Extractor.ExtractResult> extractResults) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assertions", assertResults);
        m.put("extracts", extractResults);
        return m;
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"json_serialize_failed\"}";
        }
    }

    private String resolveExecutor(Long userId) {
        try {
            User u = userMapper.selectById(userId);
            if (u == null) return "user-" + userId;
            return (u.getNickname() != null && !u.getNickname().isEmpty()) ? u.getNickname() : u.getUsername();
        } catch (Exception e) {
            return "user-" + userId;
        }
    }
}
