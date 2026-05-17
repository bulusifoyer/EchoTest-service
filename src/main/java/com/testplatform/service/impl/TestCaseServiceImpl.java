package com.testplatform.service.impl;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Assertion;
import com.testplatform.entity.Project;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestStep;
import com.testplatform.entity.VariableExtract;
import com.testplatform.entity.dto.AssertionSaveDTO;
import com.testplatform.entity.dto.ExtractSaveDTO;
import com.testplatform.entity.dto.StepSaveDTO;
import com.testplatform.entity.dto.TestCaseDetailVO;
import com.testplatform.entity.dto.TestCaseSaveDTO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.AssertionMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.mapper.TestStepMapper;
import com.testplatform.mapper.VariableExtractMapper;
import com.testplatform.service.TestCaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测试用例聚合业务实现
 * 一次性管理：用例 + 步骤 + 变量提取 + 断言
 *
 * 数据隔离：
 *   1. 用例：通过 t_test_case.create_by 直接隔离
 *   2. 步骤引用的接口：通过 t_api_definition.create_by 二次校验
 *   3. 项目：通过 t_project.create_by 校验
 *
 * 更新策略：全量替换（先删后建），避免复杂的差量算法
 * 删除策略：用例软删，步骤/变量/断言物理删除（避免外键孤儿）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Slf4j
@Service
public class TestCaseServiceImpl implements TestCaseService {

    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private TestStepMapper testStepMapper;
    @Autowired
    private VariableExtractMapper variableExtractMapper;
    @Autowired
    private AssertionMapper assertionMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private ApiDefinitionMapper apiDefinitionMapper;

    /**
     * 创建用例（聚合）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(TestCaseSaveDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 1. 项目归属校验
        Project project = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限操作");
        }

        // 2. 步骤的 apiId 归属预校验：避免后续逐步插入失败时回滚的资源浪费
        Result<Void> apiCheck = checkStepApiOwnership(dto.getSteps(), currentUserId);
        if (apiCheck != null) {
            return Result.error(apiCheck.getMessage());
        }

        // 3. 插入用例主记录
        TestCase entity = TestCase.builder()
                .projectId(dto.getProjectId())
                .createBy(currentUserId)
                .caseName(dto.getCaseName().trim())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .build();
        testCaseMapper.insert(entity);
        Long caseId = entity.getId();

        // 4. 写入步骤树
        writeStepTree(caseId, dto.getSteps());

        log.info("用例创建成功，caseId：{}，projectId：{}，stepsCount：{}",
                caseId, dto.getProjectId(), dto.getSteps().size());
        return Result.success("用例创建成功", caseId);
    }

    /**
     * 查询用例详情（含完整步骤树）
     */
    @Override
    public Result<TestCaseDetailVO> getDetail(Long id) {
        if (id == null) {
            return Result.error("用例ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        TestCase tc = testCaseMapper.selectByIdAndCreateBy(id, currentUserId);
        if (tc == null) {
            return Result.error("用例不存在或无权限访问");
        }

        // 顶层 VO
        TestCaseDetailVO vo = TestCaseDetailVO.ofCase(tc);

        // 步骤列表
        List<TestStep> steps = testStepMapper.selectListByCaseIdOrderByStepOrder(id);
        if (steps.isEmpty()) {
            vo.setSteps(Collections.emptyList());
            return Result.success(vo);
        }

        // 批量查所有步骤的 extracts 与 assertions（避免 N+1）
        List<Long> stepIds = steps.stream().map(TestStep::getId).collect(Collectors.toList());
        List<VariableExtract> allExtracts = variableExtractMapper.selectListByStepIds(stepIds);
        List<Assertion> allAssertions = assertionMapper.selectListByStepIds(stepIds);

        // 按 stepId 分组
        Map<Long, List<VariableExtract>> extractsByStepId = new HashMap<>();
        for (VariableExtract e : allExtracts) {
            extractsByStepId.computeIfAbsent(e.getStepId(), k -> new ArrayList<>()).add(e);
        }
        Map<Long, List<Assertion>> assertionsByStepId = new HashMap<>();
        for (Assertion a : allAssertions) {
            assertionsByStepId.computeIfAbsent(a.getStepId(), k -> new ArrayList<>()).add(a);
        }

        // 装配 StepVO 列表
        List<TestCaseDetailVO.StepVO> stepVOs = new ArrayList<>(steps.size());
        for (TestStep step : steps) {
            TestCaseDetailVO.StepVO svo = TestCaseDetailVO.StepVO.ofStep(step);
            svo.setExtracts(extractsByStepId.getOrDefault(step.getId(), Collections.emptyList()));
            svo.setAssertions(assertionsByStepId.getOrDefault(step.getId(), Collections.emptyList()));
            stepVOs.add(svo);
        }
        vo.setSteps(stepVOs);

        return Result.success(vo);
    }

    /**
     * 查询项目下用例列表（轻量，不含步骤）
     */
    @Override
    public Result<List<TestCase>> listByProjectId(Long projectId) {
        if (projectId == null) {
            return Result.error("项目ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        Project project = projectMapper.selectByIdAndCreateBy(projectId, currentUserId);
        if (project == null) {
            return Result.error("项目不存在或无权限访问");
        }
        List<TestCase> list = testCaseMapper.selectListByProjectAndCreateBy(projectId, currentUserId);
        return Result.success(list);
    }

    /**
     * 全量替换更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, TestCaseSaveDTO dto) {
        if (id == null) {
            return Result.error("用例ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 1. 用例归属校验
        TestCase existing = testCaseMapper.selectByIdAndCreateBy(id, currentUserId);
        if (existing == null) {
            return Result.error("用例不存在或无权限修改");
        }

        // 2. 若切换 projectId，校验新项目归属
        if (!dto.getProjectId().equals(existing.getProjectId())) {
            Project newProject = projectMapper.selectByIdAndCreateBy(dto.getProjectId(), currentUserId);
            if (newProject == null) {
                return Result.error("目标项目不存在或无权限操作");
            }
        }

        // 3. 步骤的 apiId 归属预校验
        Result<Void> apiCheck = checkStepApiOwnership(dto.getSteps(), currentUserId);
        if (apiCheck != null) {
            return Result.error(apiCheck.getMessage());
        }

        // 4. 删旧子表数据
        deleteChildrenOfCase(id);

        // 5. 更新用例主记录（caseName / description / projectId / updateTime）
        TestCase update = TestCase.builder()
                .id(id)
                .projectId(dto.getProjectId())
                .caseName(dto.getCaseName().trim())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .build();
        testCaseMapper.updateById(update);

        // 6. 重写步骤树
        writeStepTree(id, dto.getSteps());

        log.info("用例更新成功，caseId：{}，stepsCount：{}", id, dto.getSteps().size());
        return Result.success("用例更新成功", null);
    }

    /**
     * 删除用例：用例软删，子表硬删
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.error("用例ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        TestCase existing = testCaseMapper.selectByIdAndCreateBy(id, currentUserId);
        if (existing == null) {
            return Result.error("用例不存在或无权限删除");
        }

        // 先清子表
        deleteChildrenOfCase(id);
        // 软删用例（@TableLogic 自动处理）
        testCaseMapper.deleteById(id);

        log.info("用例删除成功，caseId：{}", id);
        return Result.success("用例删除成功", null);
    }

    // ========== 内部辅助方法 ==========

    /**
     * 校验步骤数组中所有 apiId 都归属当前用户
     * @return null 表示校验通过；非 null 时 message 字段含错误描述
     */
    private Result<Void> checkStepApiOwnership(List<StepSaveDTO> steps, Long currentUserId) {
        for (int i = 0; i < steps.size(); i++) {
            StepSaveDTO step = steps.get(i);
            ApiDefinition api = apiDefinitionMapper.selectByIdAndCreateBy(step.getApiId(), currentUserId);
            if (api == null) {
                return Result.error("第 " + (i + 1) + " 步引用的接口不存在或无权限访问（apiId="
                        + step.getApiId() + "）");
            }
        }
        return null;
    }

    /**
     * 写入用例的完整步骤树（步骤 + 每步的变量提取 + 每步的断言）
     * stepOrder 由后端按数组下标自动从 1 开始重排，前端传值忽略
     */
    private void writeStepTree(Long caseId, List<StepSaveDTO> steps) {
        for (int i = 0; i < steps.size(); i++) {
            StepSaveDTO sdto = steps.get(i);
            TestStep step = TestStep.builder()
                    .caseId(caseId)
                    .apiId(sdto.getApiId())
                    .stepOrder(i + 1)
                    .stepName(sdto.getStepName())
                    .overrideRequestBody(sdto.getOverrideRequestBody())
                    .build();
            testStepMapper.insert(step);
            Long stepId = step.getId();

            // 写入该步的变量提取
            if (sdto.getExtracts() != null) {
                for (ExtractSaveDTO edto : sdto.getExtracts()) {
                    VariableExtract ve = VariableExtract.builder()
                            .stepId(stepId)
                            .variableName(edto.getVariableName().trim())
                            .jsonPath(edto.getJsonPath().trim())
                            .build();
                    variableExtractMapper.insert(ve);
                }
            }

            // 写入该步的断言
            if (sdto.getAssertions() != null) {
                for (AssertionSaveDTO adto : sdto.getAssertions()) {
                    Assertion as = Assertion.builder()
                            .stepId(stepId)
                            .assertType(adto.getAssertType())
                            // expression 在 DB 是 NOT NULL；STATUS_CODE 类型常省略此字段，统一兜底为空串
                            .expression(adto.getExpression() != null ? adto.getExpression() : "")
                            .expectedValue(adto.getExpectedValue())
                            .operator(adto.getOperator() != null ? adto.getOperator() : "EQUALS")
                            .build();
                    assertionMapper.insert(as);
                }
            }
        }
    }

    /**
     * 删除某用例下的全部子表数据（步骤 + 变量提取 + 断言）
     * 顺序：先取所有 stepIds → 删 extracts/assertions → 删 steps
     */
    private void deleteChildrenOfCase(Long caseId) {
        List<TestStep> oldSteps = testStepMapper.selectListByCaseIdOrderByStepOrder(caseId);
        if (!oldSteps.isEmpty()) {
            List<Long> oldStepIds = oldSteps.stream().map(TestStep::getId).collect(Collectors.toList());
            variableExtractMapper.deleteByStepIds(oldStepIds);
            assertionMapper.deleteByStepIds(oldStepIds);
        }
        testStepMapper.deleteByCaseId(caseId);
    }
}
