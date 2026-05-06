package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.UserLoginDTO;
import com.testplatform.entity.dto.UserRegisterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户服务测试类
 * 测试用户注册和登录功能
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@SpringBootTest  // 启用Spring Boot测试支持
public class UserServiceTest {

    @Autowired
    private UserService userService;

    /**
     * 测试用户注册功能
     */
    @Test
    public void testRegister() {
        // 创建注册DTO
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("123456");
        registerDTO.setConfirmPassword("123456");
        registerDTO.setNickname("测试用户");
        registerDTO.setEmail("test@example.com");

        // 调用注册方法
        Result<Long> result = userService.register(registerDTO);

        // 打印结果
        System.out.println("注册结果：" + result);
        System.out.println("状态码：" + result.getCode());
        System.out.println("消息：" + result.getMessage());
        System.out.println("数据：" + result.getData());
    }

    /**
     * 测试用户登录功能
     */
    @Test
    public void testLogin() {
        // 创建登录DTO
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("123456");

        // 调用登录方法
        Result<String> result = userService.login(loginDTO);

        // 打印结果
        System.out.println("登录结果：" + result);
        System.out.println("状态码：" + result.getCode());
        System.out.println("消息：" + result.getMessage());
        System.out.println("Token：" + result.getData());
    }

    /**
     * 测试密码加密
     */
    @Test
    public void testPasswordEncryption() {
        String rawPassword = "123456";
        System.out.println("原始密码：" + rawPassword);

        // 使用PasswordUtils进行加密
        String encoded1 = com.testplatform.common.PasswordUtils.encode(rawPassword);
        System.out.println("第一次加密结果：" + encoded1);

        // 第二次加密
        String encoded2 = com.testplatform.common.PasswordUtils.encode(rawPassword);
        System.out.println("第二次加密结果：" + encoded2);

        // 验证密码
        boolean matches1 = com.testplatform.common.PasswordUtils.matches(rawPassword, encoded1);
        boolean matches2 = com.testplatform.common.PasswordUtils.matches(rawPassword, encoded2);

        System.out.println("第一次加密结果匹配：" + matches1);
        System.out.println("第二次加密结果匹配：" + matches2);
    }

    /**
     * 测试用户名存在性检查
     */
    @Test
    public void testUsernameExists() {
        String username = "testuser";
        boolean exists = userService.isUsernameExists(username);
        System.out.println("用户名 " + username + " 是否存在：" + exists);
    }

    /**
     * 测试邮箱存在性检查
     */
    @Test
    public void testEmailExists() {
        String email = "test@example.com";
        boolean exists = userService.isEmailExists(email);
        System.out.println("邮箱 " + email + " 是否存在：" + exists);
    }
}