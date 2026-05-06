package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.testplatform.common.JwtUtils;
import com.testplatform.common.PasswordUtils;
import com.testplatform.common.Result;
import com.testplatform.entity.User;
import com.testplatform.entity.dto.UserLoginDTO;
import com.testplatform.entity.dto.UserRegisterDTO;
import com.testplatform.mapper.UserMapper;
import com.testplatform.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户业务实现类
 * 实现用户相关的业务逻辑，包括注册、登录等操作
 * 继承ServiceImpl获得MyBatis-Plus提供的CRUD方法
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户注册
     * 处理用户注册业务逻辑
     *
     * @param dto 用户注册数据传输对象
     * @return 注册结果，成功时返回用户ID，失败时返回错误信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 事务管理，发生异常时回滚
    public Result<Long> register(UserRegisterDTO dto) {
        try {
            // 1. 参数校验
            if (dto == null) {
                return Result.error("注册信息不能为空");
            }

            // 2. 验证两次输入的密码是否一致
            if (!dto.isPasswordMatch()) {
                return Result.error("两次输入的密码不一致");
            }

            // 3. 检查密码强度
            if (!PasswordUtils.isPasswordValid(dto.getPassword())) {
                return Result.error("密码强度不符合要求，密码长度至少6位，且不能全是数字或字母");
            }

            // 4. 检查用户名是否已存在
            if (isUsernameExists(dto.getUsername())) {
                return Result.error("用户名已存在，请更换其他用户名");
            }

            // 5. 如果填写了邮箱，检查邮箱是否已存在
            if (StringUtils.hasText(dto.getEmail()) && isEmailExists(dto.getEmail())) {
                return Result.error("邮箱已被注册，请更换其他邮箱或直接登录");
            }

            // 6. 创建用户实体对象
            User user = dto.toUser();

            // 7. 对密码进行加密
            String encodedPassword = PasswordUtils.encode(dto.getPassword());
            user.setPassword(encodedPassword);

            // 8. 设置默认头像（如果未上传）
            if (!StringUtils.hasText(user.getAvatar())) {
                user.setAvatar("/assets/default-avatar.png");
            }

            // 9. 保存用户信息到数据库
            int insertResult = userMapper.insert(user);
            if (insertResult != 1) {
                log.error("用户注册失败，数据库插入失败，用户名：{}", dto.getUsername());
                return Result.error("注册失败，请稍后重试");
            }

            // 10. 记录注册成功日志
            log.info("用户注册成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());

            // 11. 返回注册结果
            return Result.success("注册成功", user.getId());

        } catch (Exception e) {
            // 记录异常日志
            log.error("用户注册发生异常，用户名：{}，错误信息：{}", dto.getUsername(), e.getMessage(), e);
            // 事务会自动回滚
            return Result.error("注册失败，系统异常");
        }
    }

    /**
     * 用户登录
     * 处理用户登录业务逻辑
     *
     * @param dto 用户登录数据传输对象
     * @return 登录结果，成功时返回JWT令牌，失败时返回错误信息
     */
    @Override
    public Result<String> login(UserLoginDTO dto) {
        try {
            // 1. 参数校验
            if (dto == null) {
                return Result.error("登录信息不能为空");
            }

            // 2. 参数标准化处理（去除空格、统一大小写等）
            dto.normalize();

            // 3. 根据用户名查询用户
            User user = userMapper.selectByUsername(dto.getUsername());
            if (user == null) {
                // 用户名不存在，返回通用错误信息（防止用户名枚举攻击）
                return Result.error("用户名或密码错误");
            }

            // 4. 检查账号是否已被删除
            if (user.getIsDeleted() == 1) {
                return Result.error("该账号已被删除");
            }

            // 5. 检查账号状态是否正常
            if (!user.isAccountActive()) {
                return Result.error("账号已被停用，请联系管理员");
            }

            // 6. 验证密码是否正确
            if (!PasswordUtils.matches(dto.getPassword(), user.getPassword())) {
                // 密码错误，记录登录失败日志
                log.warn("用户登录失败，密码错误，用户名：{}", dto.getUsername());
                return Result.error("用户名或密码错误");
            }

            // 7. 更新最后登录时间
            user.updateLastLoginTime();
            userMapper.updateById(user);

            // 8. 生成JWT令牌
            String token = jwtUtils.generateToken(user.getUsername(), user.getId());

            // 9. 记录登录成功日志
            log.info("用户登录成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());

            // 10. 返回登录结果（包含JWT令牌）
            return Result.success("登录成功", token);

        } catch (Exception e) {
            // 记录异常日志
            log.error("用户登录发生异常，用户名：{}，错误信息：{}", dto.getUsername(), e.getMessage(), e);
            return Result.error("登录失败，系统异常");
        }
    }

    /**
     * 根据用户名查询用户是否存在
     * @param username 用户名
     * @return 用户是否存在
     */
    @Override
    public boolean isUsernameExists(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userMapper.existsByUsername(username);
    }

    /**
     * 根据邮箱查询用户是否存在
     * @param email 邮箱地址
     * @return 用户是否存在
     */
    @Override
    public boolean isEmailExists(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return userMapper.existsByEmail(email);
    }

    /**
     * 根据用户ID查询用户名
     * @param userId 用户ID
     * @return 用户名
     */
    @Override
    public String getUsernameById(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }

    /**
     * 刷新用户令牌
     * 当令牌即将过期时，可以使用旧令牌获取新令牌
     * @param oldToken 旧的JWT令牌
     * @return 新的JWT令牌
     */
    @Override
    public Result<String> refreshToken(String oldToken) {
        try {
            // 1. 验证旧令牌是否有效
            if (!jwtUtils.validateToken(oldToken)) {
                return Result.error("令牌无效或已过期");
            }

            // 2. 从旧令牌中获取用户信息
            String username = jwtUtils.getUsernameFromToken(oldToken);
            Long userId = jwtUtils.getUserIdFromToken(oldToken);

            // 3. 检查用户是否存在且状态正常
            User user = userMapper.selectById(userId);
            if (user == null || !user.isAccountActive()) {
                return Result.error("用户不存在或账号状态异常");
            }

            // 4. 生成新令牌
            String newToken = jwtUtils.generateToken(username, userId);

            // 5. 返回新令牌
            return Result.success("令牌刷新成功", newToken);

        } catch (Exception e) {
            log.error("刷新令牌发生异常，错误信息：{}", e.getMessage(), e);
            return Result.error("刷新令牌失败");
        }
    }

    /**
     * 用户退出登录
     * 处理用户退出逻辑，如清除缓存、记录日志等
     * @param userId 用户ID
     * @param token 当前用户的JWT令牌
     * @return 退出结果
     */
    @Override
    public Result<Void> logout(Long userId, String token) {
        try {
            // 1. 记录退出日志
            log.info("用户退出登录，用户ID：{}", userId);

            // 2. TODO: 可以在这里添加其他退出逻辑
            //    - 将令牌加入黑名单，防止被再次使用
            //    - 清除用户的缓存信息
            //    - 记录退出时间等

            // 3. 返回退出成功结果
            return Result.success("退出登录成功", null);

        } catch (Exception e) {
            log.error("用户退出登录发生异常，用户ID：{}，错误信息：{}", userId, e.getMessage(), e);
            return Result.error("退出登录失败");
        }
    }
}