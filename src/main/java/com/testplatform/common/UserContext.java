package com.testplatform.common;

/**
 * 用户上下文工具类
 * 基于 ThreadLocal 存储当前登录用户的ID，实现请求线程级别的用户隔离
 * 配合 JwtAuthenticationInterceptor 拦截器使用，在请求进入 Controller 前自动设置
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
public class UserContext {

    /**
     * ThreadLocal 变量，用于存储当前线程的登录用户ID
     * 每个HTTP请求会由一个独立的线程处理，因此可以实现请求级别的隔离
     */
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    /**
     * 设置当前登录用户ID
     * 通常在拦截器解析JWT成功后调用
     *
     * @param userId 当前登录用户的ID
     */
    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前登录用户ID
     * 在 Service 层进行数据隔离校验时调用
     *
     * @return 当前登录用户的ID，如果未登录则返回 null
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 清除当前线程的用户ID
     * 防止线程池复用导致的数据污染，必须在请求结束时调用
     * 通常在拦截器的 afterCompletion 方法中调用
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
