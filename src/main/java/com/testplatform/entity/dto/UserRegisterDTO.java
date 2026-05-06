package com.testplatform.entity.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册数据传输对象
 * 用于接收前端用户注册请求的参数
 * 包含参数校验规则，确保数据的合法性
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
public class UserRegisterDTO {

    /**
     * 登录账号
     * 必填项，长度3-20位，只能包含字母、数字和下划线
     */
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 20, message = "登录账号长度必须在3-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "登录账号只能包含字母、数字和下划线")
    private String username;

    /**
     * 登录密码
     * 必填项，长度至少6位，不能包含空格
     */
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, message = "登录密码长度至少为6位")
    @Pattern(regexp = "^\\S+$", message = "登录密码不能包含空格")
    private String password;

    /**
     * 确认密码
     * 必填项，必须与密码字段完全一致
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 用户昵称
     * 选填项，长度1-30位
     */
    @Size(max = 30, message = "用户昵称长度不能超过30位")
    private String nickname;

    /**
     * 邮箱地址
     * 选填项，但必须符合邮箱格式
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100位")
    private String email;

    /**
     * 验证码
     * 用于防止恶意注册
     */
    private String captcha;

    /**
     * 验证两次输入的密码是否一致
     * @return 密码是否一致
     */
    public boolean isPasswordMatch() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * 将DTO转换为User实体
     * 只转换必要的字段，排除确认密码和验证码
     * @return User实体对象
     */
    public com.testplatform.entity.User toUser() {
        return com.testplatform.entity.User.builder()
                .username(this.username)
                .password(this.password)  // 密码将在Service层加密
                .nickname(this.nickname)
                .email(this.email)
                .status(1)  // 默认状态为正常
                .isDeleted(0)  // 默认未删除
                .build();
    }
}