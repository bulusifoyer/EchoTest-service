package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 测试步骤保存数据传输对象
 * 用于在 TestCaseSaveDTO 内部，作为步骤数组元素
 * 步骤的执行顺序由后端按数组顺序自动重排，前端无需传 stepOrder
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class StepSaveDTO {

    /**
     * 引用的接口ID
     * 必填，且必须归属当前登录用户
     */
    @NotNull(message = "接口ID不能为空")
    private Long apiId;

    /**
     * 步骤别名
     * 选填，长度不超过100位
     */
    @Size(max = 100, message = "步骤别名长度不能超过100位")
    private String stepName;

    /**
     * 覆盖请求体
     * 选填，可包含 ${var} 变量占位（M3 执行引擎处理）
     */
    private String overrideRequestBody;

    /**
     * 变量提取规则集合
     * 选填，每条提取一个变量
     */
    @Valid
    private List<ExtractSaveDTO> extracts;

    /**
     * 断言规则集合
     * 选填，但建议至少一条以便能判定步骤通过/失败
     */
    @Valid
    private List<AssertionSaveDTO> assertions;
}
