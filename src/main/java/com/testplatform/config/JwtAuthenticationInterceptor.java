package com.testplatform.config;

import com.testplatform.common.JwtUtils;
import com.testplatform.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * 拦截所有需要登录的接口请求，从请求头中提取并验证 JWT 令牌
 * 验证通过后，将当前登录用户ID存入 ThreadLocal，供后续业务层使用
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 请求处理前执行的拦截逻辑
     * 从 Authorization 请求头中提取 Bearer Token，解析并验证用户身份
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param handler  处理器对象
     * @return 是否继续执行后续流程，true-继续，false-中断
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头中获取 Authorization 字段
        String authHeader = request.getHeader("Authorization");

        // 2. 如果请求头为空或不以 "Bearer " 开头，则视为未登录
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少有效的 Authorization 头，URI：{}", request.getRequestURI());
            // 返回401未授权状态码
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3. 提取 JWT 令牌（移除 "Bearer " 前缀）
        String token = authHeader.substring(7);

        try {
            // 4. 验证令牌是否有效
            if (!jwtUtils.validateToken(token)) {
                log.warn("JWT 令牌验证失败，URI：{}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 5. 从令牌中解析用户ID，并存入 ThreadLocal
            Long userId = jwtUtils.getUserIdFromToken(token);
            UserContext.setCurrentUserId(userId);

            log.debug("JWT 认证通过，用户ID：{}，URI：{}", userId, request.getRequestURI());
            return true;

        } catch (Exception e) {
            log.error("JWT 解析异常，URI：{}，错误：{}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    /**
     * 请求完成后执行的清理逻辑
     * 清除 ThreadLocal 中存储的用户ID，防止线程池复用导致的数据泄漏
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param handler  处理器对象
     * @param ex       处理过程中抛出的异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后必须清理 ThreadLocal，避免线程复用时出现数据污染
        UserContext.clear();
        log.debug("请求结束，清理用户上下文，URI：{}", request.getRequestURI());
    }
}
