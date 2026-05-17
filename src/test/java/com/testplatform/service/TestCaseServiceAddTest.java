package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.dto.AssertionSaveDTO;
import com.testplatform.entity.dto.ExtractSaveDTO;
import com.testplatform.entity.dto.StepSaveDTO;
import com.testplatform.entity.dto.TestCaseSaveDTO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.AssertionMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.mapper.TestStepMapper;
import com.testplatform.mapper.VariableExtractMapper;
import com.testplatform.service.impl.TestCaseServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TestCaseService.add 主路径单元测试
 *
 * 验收测试点：跨用户提交项目ID → 应返回"项目不存在或无权限操作"，
 * 且任何子表（用例/步骤/变量/断言）都不发生 insert
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@ExtendWith(MockitoExtension.class)
public class TestCaseServiceAddTest {

    @Mock
    private TestCaseMapper testCaseMapper;
    @Mock
    private TestStepMapper testStepMapper;
    @Mock
    private VariableExtractMapper variableExtractMapper;
    @Mock
    private AssertionMapper assertionMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ApiDefinitionMapper apiDefinitionMapper;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void testAdd_DataIsolation_RejectCrossUserProject() {
        // 模拟用户 B 登录
        UserContext.setCurrentUserId(2L);
        // projectMapper 在用 (projectId=100, createBy=2) 查询时返回 null —— 表示项目不属于用户 B
        when(projectMapper.selectByIdAndCreateBy(anyLong(), anyLong())).thenReturn(null);

        // 准备 DTO（projectId=100 假设属于用户 A）
        ExtractSaveDTO ext = new ExtractSaveDTO();
        ext.setVariableName("token");
        ext.setJsonPath("$.data");

        AssertionSaveDTO asn = new AssertionSaveDTO();
        asn.setAssertType("STATUS_CODE");
        asn.setExpectedValue("200");
        asn.setOperator("EQUALS");

        StepSaveDTO step = new StepSaveDTO();
        step.setApiId(11L);
        step.setStepName("Step-1");
        step.setExtracts(Collections.singletonList(ext));
        step.setAssertions(Collections.singletonList(asn));

        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setProjectId(100L);
        dto.setCaseName("跨用户用例");
        dto.setSteps(Collections.singletonList(step));

        // 执行
        Result<Long> result = testCaseService.add(dto);

        // 断言：返回无权限错误
        assertNotNull(result);
        assertEquals(Integer.valueOf(500), result.getCode());
        assertEquals("项目不存在或无权限操作", result.getMessage());

        // 关键：任何子表 insert 都不应被调用
        verify(testCaseMapper, never()).insert(any());
        verify(testStepMapper, never()).insert(any());
        verify(variableExtractMapper, never()).insert(any());
        verify(assertionMapper, never()).insert(any());
        // 接口归属校验也不应被触发（项目校验先失败）
        verify(apiDefinitionMapper, never()).selectByIdAndCreateBy(anyLong(), anyLong());
    }
}
