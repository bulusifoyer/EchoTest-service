package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 测试用例聚合保存数据传输对象
 * 一次性提交：用例 + 所有步骤 + 每步的变量提取与断言
 * add 与 update 共用此结构（update 时 id 走 PathVariable，故此 DTO 不带 id）
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
public class TestCaseSaveDTO {

    /**
     * 所属项目ID
     * 必填，且必须归属当前登录用户
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 用例名称
     */
    @NotBlank(message = "用例名称不能为空")
    @Size(max = 100, message = "用例名称长度不能超过100位")
    private String caseName;

    /**
     * 用例描述
     * 选填，长度不超过255位
     */
    @Size(max = 255, message = "用例描述长度不能超过255位")
    private String description;

    /**
     * 步骤列表
     * 必填且至少一个；执行顺序由后端按数组下标自动重排
     */
    @NotNull(message = "步骤列表不能为空")
    @Size(min = 1, message = "用例至少包含一个步骤")
    @Valid
    private List<StepSaveDTO> steps;
}
