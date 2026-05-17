package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 变量提取保存数据传输对象
 * 用于在 TestCaseSaveDTO 内部，作为某一步骤的变量提取数组元素
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class ExtractSaveDTO {

    /**
     * 变量名
     * 后续步骤通过 ${variableName} 引用
     */
    @NotBlank(message = "变量名不能为空")
    @Size(max = 50, message = "变量名长度不能超过50位")
    private String variableName;

    /**
     * JSONPath 表达式
     */
    @NotBlank(message = "JSONPath 不能为空")
    @Size(max = 255, message = "JSONPath 长度不能超过255位")
    private String jsonPath;
}
