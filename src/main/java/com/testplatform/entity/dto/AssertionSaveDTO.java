package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 断言保存数据传输对象
 * 用于在 TestCaseSaveDTO 内部，作为某一步骤的断言数组元素
 *
 * 校验：
 *   - assertType 仅支持 STATUS_CODE / JSON_PATH（DATABASE 类型为规划项）
 *   - operator 仅支持 EQUALS / CONTAINS / GREATER_THAN
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class AssertionSaveDTO {

    /**
     * 断言类型
     */
    @NotBlank(message = "断言类型不能为空")
    @Pattern(regexp = "^(STATUS_CODE|JSON_PATH)$", message = "断言类型仅支持 STATUS_CODE / JSON_PATH")
    private String assertType;

    /**
     * 断言表达式
     * STATUS_CODE 时可留空；JSON_PATH 时存放 JSONPath，如 $.code
     */
    private String expression;

    /**
     * 期望值
     */
    @NotBlank(message = "期望值不能为空")
    private String expectedValue;

    /**
     * 比较运算符；默认 EQUALS
     */
    @Pattern(regexp = "^(EQUALS|CONTAINS|GREATER_THAN)$", message = "比较运算符仅支持 EQUALS / CONTAINS / GREATER_THAN")
    private String operator = "EQUALS";
}
