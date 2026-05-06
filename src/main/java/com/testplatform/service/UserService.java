package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.UserLoginDTO;
import com.testplatform.entity.dto.UserRegisterDTO;

/**
 * 用户业务接口
 * 定义用户相关的业务操作，包括注册、登录等
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
public interface UserService {

    /**
     * 用户注册
     * 处理用户注册业务逻辑，包括：
     * 1. 校验注册参数的合法性
     * 2. 检查用户名是否已存在
     * 3. 对密码进行加密处理
     * 4. 保存用户信息到数据库
     *
     * @param dto 用户注册数据传输对象
     * @return 注册结果，成功时返回用户ID，失败时返回错误信息
     */
    Result<Long> register(UserRegisterDTO dto);

    /**
     * 用户登录
     * 处理用户登录业务逻辑，包括：
     * 1. 校验登录参数的合法性
     * 2. 根据用户名查询用户信息
     * 3. 验证密码是否正确
     * 4. 检查账号状态是否正常
     * 5. 更新最后登录时间
     * 6. 生成JWT令牌
     *
     * @param dto 用户登录数据传输对象
     * @return 登录结果，成功时返回JWT令牌，失败时返回错误信息
     */
    Result<String> login(UserLoginDTO dto);

    /**
     * 根据用户名查询用户是否存在
     * @param username 用户名
     * @return 用户是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 根据邮箱查询用户是否存在
     * @param email 邮箱地址
     * @return 用户是否存在
     */
    boolean isEmailExists(String email);

    /**
     * 根据用户ID查询用户名
     * @param userId 用户ID
     * @return 用户名
     */
    String getUsernameById(Long userId);

    /**
     * 刷新用户令牌
     * 当令牌即将过期时，可以使用旧令牌获取新令牌
     * @param oldToken 旧的JWT令牌
     * @return 新的JWT令牌
     */
    Result<String> refreshToken(String oldToken);

    /**
     * 用户退出登录
     * 处理用户退出逻辑，如清除缓存、记录日志等
     * @param userId 用户ID
     * @param token 当前用户的JWT令牌
     * @return 退出结果
     */
    Result<Void> logout(Long userId, String token);
}