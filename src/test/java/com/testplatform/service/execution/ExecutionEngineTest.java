package com.testplatform.service.execution;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExecutionEngine 集成单元测试（Mockito，全 Mock 数据库）
 *
 * 场景：构造一个 2 步用例
 *   step1：GET /uuid，从 $.uuid 提取变量 token
 *   step2：GET /get?token=${token}，断言 status=200 且 $.args.token EQUALS extracted-uuid
 *
 * Mock HttpExecutor：
 *   step1 返回 200 + body {"uuid":"xyz-001"}
 *   step2 返回 200 + body {"args":{"token":"xyz-001"}}
 *
 * 期望：
 *   - 两步都通过；report.status=PASSED；passedSteps=2；failedSteps=0
 *   - step2 实际执行的 URL 必须是变量替换后的 /get?token=xyz-001（不再是 ${token}）
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@ExtendWith(MockitoExtension.class)
public class ExecutionEngineTest {

    @Mock private TestCaseMapper testCaseMapper;
    @Mock private TestStepMapper testStepMapper;
    @Mock private VariableExtractMapper variableExtractMapper;
    @Mock private AssertionMapper assertionMapper;
    @Mock private ApiDefinitionMapper apiDefinitionMapper;
    @Mock private EnvironmentMapper environmentMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ExecutionReportMapper reportMapper;
    @Mock private ExecutionDetailMapper detailMapper;
    @Mock private UserMapper userMapper;
    @Mock private HttpExecutor httpExecutor;

    @InjectMocks
    private ExecutionEngine engine;

    private final Long currentUserId = 100L;
    private final Long projectId = 9000L;
    private final Long caseId = 1000L;
    private final Long envId = 2000L;

    @BeforeEach
    void setupCommon() {
        // 用例归属
        TestCase tc = TestCase.builder()
                .id(caseId).projectId(projectId).createBy(currentUserId)
                .caseName("登录后取信息").description("e2e").isDeleted(0).build();
        when(testCaseMapper.selectByIdAndCreateBy(eq(caseId), eq(currentUserId))).thenReturn(tc);

        // 环境
        Environment env = Environment.builder()
                .id(envId).projectId(projectId)
                .envName("local")
                .baseUrl("http://stub.local")
                .globalHeaders(null)
                .build();
        when(environmentMapper.selectById(envId)).thenReturn(env);

        Project envProject = Project.builder()
                .id(projectId).name("Demo").createBy(currentUserId).isDeleted(0).build();
        when(projectMapper.selectByIdAndCreateBy(eq(projectId), eq(currentUserId))).thenReturn(envProject);

        // 用户（用于 executor 字段）
        User user = User.builder().id(currentUserId).username("alice").nickname("Alice").build();
        when(userMapper.selectById(currentUserId)).thenReturn(user);
    }

    @Test
    void runTwoStepCaseWithVariableExtractAndAssertions() {
        // ----- 步骤 -----
        TestStep step1 = TestStep.builder()
                .id(1L).caseId(caseId).apiId(11L).stepOrder(1).stepName("取 uuid").build();
        TestStep step2 = TestStep.builder()
                .id(2L).caseId(caseId).apiId(12L).stepOrder(2).stepName("带 token 调用").build();
        when(testStepMapper.selectListByCaseIdOrderByStepOrder(caseId))
                .thenReturn(Arrays.asList(step1, step2));

        // ----- 接口 -----
        ApiDefinition api1 = ApiDefinition.builder()
                .id(11L).projectId(projectId).createBy(currentUserId)
                .name("uuid").method("GET").path("/uuid").isDeleted(0).build();
        ApiDefinition api2 = ApiDefinition.builder()
                .id(12L).projectId(projectId).createBy(currentUserId)
                .name("get-with-token").method("GET").path("/get?token=${token}").isDeleted(0).build();
        when(apiDefinitionMapper.selectByIdAndCreateBy(eq(11L), eq(currentUserId))).thenReturn(api1);
        when(apiDefinitionMapper.selectByIdAndCreateBy(eq(12L), eq(currentUserId))).thenReturn(api2);

        // ----- 提取规则：step1 提 token -----
        VariableExtract ve = VariableExtract.builder()
                .id(101L).stepId(1L).variableName("token").jsonPath("$.uuid").build();
        when(variableExtractMapper.selectListByStepIds(Arrays.asList(1L, 2L)))
                .thenReturn(Collections.singletonList(ve));

        // ----- 断言：step1 status=200；step2 status=200 + $.args.token == xyz-001 -----
        Assertion a1 = Assertion.builder().id(201L).stepId(1L)
                .assertType("STATUS_CODE").operator("EQUALS").expectedValue("200").build();
        Assertion a2 = Assertion.builder().id(202L).stepId(2L)
                .assertType("STATUS_CODE").operator("EQUALS").expectedValue("200").build();
        Assertion a3 = Assertion.builder().id(203L).stepId(2L)
                .assertType("JSON_PATH").operator("EQUALS")
                .expression("$.args.token").expectedValue("xyz-001").build();
        when(assertionMapper.selectListByStepIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(a1, a2, a3));

        // ----- HttpExecutor 桩：按 url 区分 -----
        when(httpExecutor.execute(anyString(), anyString(), any(), any(), anyInt()))
                .thenAnswer(inv -> {
                    String url = inv.getArgument(1);
                    if (url.endsWith("/uuid")) {
                        return HttpExecutor.ExecuteResult.builder()
                                .statusCode(200).elapsedMs(15L)
                                .responseHeaders(new LinkedHashMap<>())
                                .responseBody("{\"uuid\":\"xyz-001\"}")
                                .success(true).build();
                    }
                    return HttpExecutor.ExecuteResult.builder()
                            .statusCode(200).elapsedMs(20L)
                            .responseHeaders(new LinkedHashMap<>())
                            .responseBody("{\"args\":{\"token\":\"xyz-001\"}}")
                            .success(true).build();
                });

        // ----- 模拟 reportMapper.insert 回填 id -----
        AtomicLong fakeReportId = new AtomicLong(7777L);
        when(reportMapper.insert(any(ExecutionReport.class))).thenAnswer(inv -> {
            ExecutionReport r = inv.getArgument(0);
            r.setId(fakeReportId.get());
            return 1;
        });

        // ----- 跑 -----
        Long reportId = engine.run(caseId, envId, 5000, currentUserId);

        // ----- 断言执行结果 -----
        assertNotNull(reportId);
        assertEquals(7777L, reportId);

        // 校验最终 report 汇总状态
        ArgumentCaptor<ExecutionReport> reportCaptor = ArgumentCaptor.forClass(ExecutionReport.class);
        verify(reportMapper).updateById(reportCaptor.capture());
        ExecutionReport finalReport = reportCaptor.getValue();
        assertEquals(ExecutionEngine.REPORT_STATUS_PASSED, finalReport.getStatus());
        assertEquals(2, finalReport.getPassedSteps());
        assertEquals(0, finalReport.getFailedSteps());
        assertNotNull(finalReport.getEndTime());

        // 校验明细写入 2 条
        ArgumentCaptor<ExecutionDetail> detailCaptor = ArgumentCaptor.forClass(ExecutionDetail.class);
        verify(detailMapper, times(2)).insert(detailCaptor.capture());

        ExecutionDetail d1 = detailCaptor.getAllValues().get(0);
        ExecutionDetail d2 = detailCaptor.getAllValues().get(1);

        assertEquals(ExecutionEngine.STEP_STATUS_PASSED, d1.getStatus());
        assertEquals("http://stub.local/uuid", d1.getRequestUrl());
        assertNull(d1.getFailReason());

        assertEquals(ExecutionEngine.STEP_STATUS_PASSED, d2.getStatus());
        // ${token} 必须被替换为 xyz-001
        assertEquals("http://stub.local/get?token=xyz-001", d2.getRequestUrl());
        assertTrue(d2.getActualResponse() != null && d2.getActualResponse().contains("xyz-001"));
    }

    @Test
    void runFailsOnAssertionAndStopsImmediately() {
        // 单步用例，断言一定不过，验证遇错即停 + report.status=FAILED
        TestStep step1 = TestStep.builder()
                .id(1L).caseId(caseId).apiId(11L).stepOrder(1).stepName("ping").build();
        when(testStepMapper.selectListByCaseIdOrderByStepOrder(caseId))
                .thenReturn(Collections.singletonList(step1));

        ApiDefinition api1 = ApiDefinition.builder()
                .id(11L).projectId(projectId).createBy(currentUserId)
                .name("ping").method("GET").path("/x").isDeleted(0).build();
        when(apiDefinitionMapper.selectByIdAndCreateBy(eq(11L), eq(currentUserId))).thenReturn(api1);

        when(variableExtractMapper.selectListByStepIds(Collections.singletonList(1L)))
                .thenReturn(Collections.emptyList());
        Assertion a1 = Assertion.builder().id(201L).stepId(1L)
                .assertType("STATUS_CODE").operator("EQUALS").expectedValue("200").build();
        when(assertionMapper.selectListByStepIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(a1));

        // 实际返回 500
        when(httpExecutor.execute(anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(HttpExecutor.ExecuteResult.builder()
                        .statusCode(500).elapsedMs(10L)
                        .responseHeaders(new LinkedHashMap<>()).responseBody("oops")
                        .success(true).build());

        when(reportMapper.insert(any(ExecutionReport.class))).thenAnswer(inv -> {
            ((ExecutionReport) inv.getArgument(0)).setId(8888L);
            return 1;
        });

        Long reportId = engine.run(caseId, envId, 5000, currentUserId);
        assertEquals(8888L, reportId);

        ArgumentCaptor<ExecutionReport> rc = ArgumentCaptor.forClass(ExecutionReport.class);
        verify(reportMapper).updateById(rc.capture());
        assertEquals(ExecutionEngine.REPORT_STATUS_FAILED, rc.getValue().getStatus());
        assertEquals(0, rc.getValue().getPassedSteps());
        assertEquals(1, rc.getValue().getFailedSteps());
    }
}
