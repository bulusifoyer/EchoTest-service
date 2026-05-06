package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 项目更新数据传输对象
 * 用于接收前端修改项目信息的请求参数
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
public class ProjectUpdateDTO {

    /**
     * 项目ID
     * 必填项，标识要更新的目标项目
     */
    @NotNull(message = "项目ID不能为空")
    private Long id;

    /**
     * 项目名称
     * 必填项，长度1-100位
     */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100位")
    private String name;

    /**
     * 项目描述
     * 选填项，长度不超过255位
     */
    @Size(max = 255, message = "项目描述长度不能超过255位")
    private String description;
}
