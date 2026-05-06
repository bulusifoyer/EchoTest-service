package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录数据传输对象
 * 用于接收前端用户登录请求的参数
 * 包含参数校验规则，确保数据的合法性
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
public class UserLoginDTO {

    /**
     * 登录账号
     * 必填项，长度3-20位
     */
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 20, message = "登录账号长度必须在3-20位之间")
    private String username;

    /**
     * 登录密码
     * 必填项，长度至少6位
     */
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, message = "登录密码长度至少为6位")
    private String password;

    /**
     * 记住我
     * 选填项，用于实现自动登录功能
     * 默认值为false
     */
    private Boolean rememberMe = false;

    /**
     * 验证码
     * 用于防止暴力破解
     * 可选，根据系统配置决定是否必填
     */
    private String captcha;

    /**
     * 验证码唯一标识
     * 用于获取对应的验证码进行校验
     */
    private String captchaId;

    /**
     * 获取记住我状态
     * @return 是否记住登录状态
     */
    public boolean isRememberMe() {
        return rememberMe != null && rememberMe;
    }

    /**
     * 转换为标准格式
     * 去除前后空格，统一大小写等
     */
    public void normalize() {
        if (username != null) {
            username = username.trim().toLowerCase();
        }
        if (password != null) {
            password = password.trim();
        }
        if (captcha != null) {
            captcha = captcha.trim();
        }
        if (captchaId != null) {
            captchaId = captchaId.trim();
        }
    }
}