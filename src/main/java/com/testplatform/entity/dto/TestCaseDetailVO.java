package com.testplatform.entity.dto;

import com.testplatform.entity.Assertion;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestStep;
import com.testplatform.entity.VariableExtract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试用例详情视图对象
 * 返回完整的用例树：用例 + 步骤数组（每步含变量提取与断言）
 * 顶层即为前端编辑页所需的全部配置
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDetailVO {

    /** 用例ID */
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 用例名称 */
    private String caseName;

    /** 用例描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 步骤列表（按 step_order 升序） */
    private List<StepVO> steps;

    /** 步骤视图对象（嵌套） */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepVO {
        /** 步骤ID */
        private Long id;
        /** 引用的接口ID */
        private Long apiId;
        /** 执行顺序 */
        private Integer stepOrder;
        /** 步骤别名 */
        private String stepName;
        /** 覆盖请求体 */
        private String overrideRequestBody;
        /** 该步骤的变量提取规则 */
        private List<VariableExtract> extracts;
        /** 该步骤的断言规则 */
        private List<Assertion> assertions;

        /** 由 TestStep 实体快速构造 StepVO（不含 extracts/assertions 字段） */
        public static StepVO ofStep(TestStep step) {
            return StepVO.builder()
                    .id(step.getId())
                    .apiId(step.getApiId())
                    .stepOrder(step.getStepOrder())
                    .stepName(step.getStepName())
                    .overrideRequestBody(step.getOverrideRequestBody())
                    .build();
        }
    }

    /** 由 TestCase 实体快速构造顶层 VO（不含 steps 字段） */
    public static TestCaseDetailVO ofCase(TestCase tc) {
        return TestCaseDetailVO.builder()
                .id(tc.getId())
                .projectId(tc.getProjectId())
                .caseName(tc.getCaseName())
                .description(tc.getDescription())
                .createTime(tc.getCreateTime())
                .updateTime(tc.getUpdateTime())
                .build();
    }
}
