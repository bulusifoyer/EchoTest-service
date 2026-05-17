package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Project;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestStep;
import com.testplatform.entity.dto.AssertionSaveDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TestCaseService.update 主路径单元测试
 *
 * 验收测试点（"全量替换"语义）：
 *   1. 旧步骤先被批量删除（extracts → assertions → steps 顺序无要求，但都要删）
 *   2. 用例主记录被 updateById
 *   3. 新的 2 个步骤被插入，每步携带其各自的子表
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@ExtendWith(MockitoExtension.class)
public class TestCaseServiceUpdateTest {

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
    void testUpdate_FullReplaceFlow() {
        UserContext.setCurrentUserId(1L);

        Long caseId = 50L;

        // 1. 既有用例属于当前用户
        TestCase existing = TestCase.builder()
                .id(caseId).projectId(100L).createBy(1L).caseName("old").build();
        when(testCaseMapper.selectByIdAndCreateBy(caseId, 1L)).thenReturn(existing);

        // 2. 项目归属：本测试不切换 projectId，所以不会触发 projectMapper 调用
        //    注意：如果切换 projectId，会再走一次 selectByIdAndCreateBy；这里不需要 mock

        // 3. 旧步骤列表
        TestStep oldStep1 = TestStep.builder().id(101L).caseId(caseId).apiId(11L).stepOrder(1).build();
        TestStep oldStep2 = TestStep.builder().id(102L).caseId(caseId).apiId(12L).stepOrder(2).build();
        when(testStepMapper.selectListByCaseIdOrderByStepOrder(caseId))
                .thenReturn(Arrays.asList(oldStep1, oldStep2));

        // 4. 新步骤引用的 api 都属于当前用户
        ApiDefinition apiOk = ApiDefinition.builder()
                .id(0L).createBy(1L).method("GET").path("/x").build();
        when(apiDefinitionMapper.selectByIdAndCreateBy(anyLong(), eq(1L))).thenReturn(apiOk);

        // 5. 准备新数据：2 个步骤，每步带 1 条断言
        AssertionSaveDTO asn = new AssertionSaveDTO();
        asn.setAssertType("STATUS_CODE");
        asn.setExpectedValue("200");
        asn.setOperator("EQUALS");

        StepSaveDTO s1 = new StepSaveDTO();
        s1.setApiId(21L);
        s1.setStepName("new-1");
        s1.setAssertions(Collections.singletonList(asn));

        StepSaveDTO s2 = new StepSaveDTO();
        s2.setApiId(22L);
        s2.setStepName("new-2");
        s2.setAssertions(Collections.singletonList(asn));

        TestCaseSaveDTO dto = new TestCaseSaveDTO();
        dto.setProjectId(100L);
        dto.setCaseName("renamed");
        dto.setDescription("new desc");
        dto.setSteps(Arrays.asList(s1, s2));

        // 执行
        Result<Void> result = testCaseService.update(caseId, dto);
        assertEquals(Integer.valueOf(200), result.getCode());

        // 6. 断言：删除子表（extracts/assertions）传入的 stepIds 是 [101, 102]
        ArgumentCaptor<List<Long>> extractCaptor = ArgumentCaptor.forClass(List.class);
        verify(variableExtractMapper, times(1)).deleteByStepIds(extractCaptor.capture());
        ArgumentCaptor<List<Long>> assertionCaptor = ArgumentCaptor.forClass(List.class);
        verify(assertionMapper, times(1)).deleteByStepIds(assertionCaptor.capture());
        assertEquals(Arrays.asList(101L, 102L), extractCaptor.getValue());
        assertEquals(Arrays.asList(101L, 102L), assertionCaptor.getValue());

        // 7. 断言：步骤被整批删除
        verify(testStepMapper, times(1)).deleteByCaseId(caseId);

        // 8. 断言：用例主记录被更新
        verify(testCaseMapper, times(1)).updateById(any(TestCase.class));

        // 9. 断言：新插入了 2 个步骤
        verify(testStepMapper, times(2)).insert(any(TestStep.class));
        // 每步 1 条断言 → 共 2 条断言被插入
        verify(assertionMapper, times(2)).insert(any());
        // 没有变量提取 → 0 次插入（lenient 默认即可）

        // 10. 顺序断言：先删子表（变量/断言）→ 删步骤 → 更新用例 → 重写步骤
        InOrder order = inOrder(variableExtractMapper, assertionMapper, testStepMapper, testCaseMapper);
        order.verify(variableExtractMapper).deleteByStepIds(anyList());
        order.verify(assertionMapper).deleteByStepIds(anyList());
        order.verify(testStepMapper).deleteByCaseId(caseId);
        order.verify(testCaseMapper).updateById(any(TestCase.class));
        order.verify(testStepMapper, times(2)).insert(any(TestStep.class));
    }
}
