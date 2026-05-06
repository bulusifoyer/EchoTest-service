package com.testplatform.controller;

import com.testplatform.common.JwtUtils;
import com.testplatform.common.Result;
import com.testplatform.entity.dto.UserLoginDTO;
import com.testplatform.entity.dto.UserRegisterDTO;
import com.testplatform.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 * 处理用户注册、登录等认证相关的HTTP请求
 * 提供RESTful API接口供前端调用
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@RestController  // 声明为REST控制器，返回JSON数据
@RequestMapping("/api/auth")  // 统一的路径前缀
@Api(tags = "用户认证接口")  // Swagger文档注解
@Validated  // 启用方法级别的参数校验
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户注册接口
     * 接收用户注册请求，处理注册业务逻辑
     *
     * @param dto 用户注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "新用户注册接口")
    public Result<Long> register(
            @Valid  // 启用参数校验
            @RequestBody  // 接收JSON格式的请求体
            @ApiParam(name = "注册信息", value = "用户注册所需信息", required = true)
            UserRegisterDTO dto
    ) {
        // 记录注册请求日志
        log.info("收到用户注册请求，用户名：{}", dto.getUsername());

        try {
            // 调用Service层的注册方法
            Result<Long> result = userService.register(dto);

            // 记录注册结果日志
            if (result.getCode() == 200) {
                log.info("用户注册成功，用户名：{}，用户ID：{}", dto.getUsername(), result.getData());
            } else {
                log.warn("用户注册失败，用户名：{}，错误信息：{}", dto.getUsername(), result.getMessage());
            }

            return result;

        } catch (Exception e) {
            // 记录异常日志
            log.error("处理用户注册请求发生异常，用户名：{}，错误信息：{}", dto.getUsername(), e.getMessage(), e);
            return Result.error("注册失败，系统异常");
        }
    }

    /**
     * 用户登录接口
     * 接收用户登录请求，验证用户身份并返回JWT令牌
     *
     * @param dto 用户登录信息
     * @param request HTTP请求对象，用于获取客户端信息
     * @return 登录结果，包含JWT令牌
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "用户登录认证接口，成功返回JWT令牌")
    public Result<String> login(
            @Valid  // 启用参数校验
            @RequestBody  // 接收JSON格式的请求体
            @ApiParam(name = "登录信息", value = "用户登录所需信息", required = true)
            UserLoginDTO dto,
            HttpServletRequest request
    ) {
        // 记录登录请求日志
        log.info("收到用户登录请求，用户名：{}，客户端IP：{}", dto.getUsername(), getClientIp(request));

        try {
            // 调用Service层的登录方法
            Result<String> result = userService.login(dto);

            // 记录登录结果日志
            if (result.getCode() == 200) {
                log.info("用户登录成功，用户名：{}", dto.getUsername());
            } else {
                log.warn("用户登录失败，用户名：{}，错误信息：{}", dto.getUsername(), result.getMessage());
            }

            return result;

        } catch (Exception e) {
            // 记录异常日志
            log.error("处理用户登录请求发生异常，用户名：{}，错误信息：{}", dto.getUsername(), e.getMessage(), e);
            return Result.error("登录失败，系统异常");
        }
    }

    /**
     * 检查用户名是否可用
     * 用于前端实时校验用户名是否已被注册
     *
     * @param username 用户名
     * @return 检查结果
     */
    @GetMapping("/check-username")
    @ApiOperation(value = "检查用户名", notes = "检查用户名是否已存在")
    public Result<Boolean> checkUsername(
            @RequestParam
            @ApiParam(name = "username", value = "用户名", required = true, example = "admin")
            String username
    ) {
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }

        // 检查用户名长度
        if (username.length() < 3 || username.length() > 20) {
            return Result.error("用户名长度必须在3-20位之间");
        }

        // 检查用户名格式
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return Result.error("用户名只能包含字母、数字和下划线");
        }

        // 查询用户名是否已存在
        boolean exists = userService.isUsernameExists(username);

        // 返回检查结果
        return Result.success("检查完成", !exists);
    }

    /**
     * 检查邮箱是否可用
     * 用于前端实时校验邮箱是否已被注册
     *
     * @param email 邮箱地址
     * @return 检查结果
     */
    @GetMapping("/check-email")
    @ApiOperation(value = "检查邮箱", notes = "检查邮箱是否已存在")
    public Result<Boolean> checkEmail(
            @RequestParam
            @ApiParam(name = "email", value = "邮箱地址", required = true, example = "test@example.com")
            String email
    ) {
        // 参数校验
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }

        // 简单的邮箱格式校验
        if (!email.contains("@") || !email.contains(".")) {
            return Result.error("邮箱格式不正确");
        }

        // 查询邮箱是否已存在
        boolean exists = userService.isEmailExists(email);

        // 返回检查结果
        return Result.success("检查完成", !exists);
    }

    /**
     * 刷新JWT令牌
     * 当令牌即将过期时，可以使用此接口获取新令牌
     *
     * @param request HTTP请求对象，用于获取当前令牌
     * @return 新的JWT令牌
     */
    @PostMapping("/refresh-token")
    @ApiOperation(value = "刷新令牌", notes = "使用旧令牌获取新令牌")
    public Result<String> refreshToken(HttpServletRequest request) {
        // 从请求头中获取当前令牌
        String oldToken = extractTokenFromRequest(request);
        if (oldToken == null) {
            return Result.error("未提供有效的JWT令牌");
        }

        // 调用Service层刷新令牌
        return userService.refreshToken(oldToken);
    }

    /**
     * 用户退出登录
     * 处理用户退出请求
     *
     * @param request HTTP请求对象，用于获取用户信息和令牌
     * @return 退出结果
     */
    @PostMapping("/logout")
    @ApiOperation(value = "用户退出", notes = "用户退出登录")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求头中获取当前令牌
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return Result.error("未提供有效的JWT令牌");
        }

        // 从令牌中获取用户ID
        Long userId;
        try {
            userId = jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("解析JWT令牌获取用户ID失败", e);
            return Result.error("令牌无效或已过期");
        }

        // 调用Service层处理退出逻辑
        return userService.logout(userId, token);
    }

    /**
     * 获取客户端IP地址
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP地址
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }

    /**
     * 从请求中提取JWT令牌
     * @param request HTTP请求对象
     * @return JWT令牌，如果未找到则返回null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 从请求头中获取令牌
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 移除"Bearer "前缀
        }
        return null;
    }
}