package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问接口
 * 继承MyBatis-Plus的BaseMapper，提供基本的CRUD操作
 * 同时支持自定义SQL查询
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * 使用MyBatis-Plus的Wrapper实现，无需编写SQL
     * 示例：User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
     */

    /**
     * 根据用户名查询用户（自定义SQL）
     * 用于演示自定义SQL的写法，实际可以使用MyBatis-Plus的条件构造器替代
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM t_user WHERE username = #{username} AND is_deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱地址
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE email = #{email} AND is_deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM t_user WHERE email = #{email} AND is_deleted = 0")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 更新用户最后登录时间
     * @param userId 用户ID
     * @param lastLoginTime 最后登录时间
     * @return 更新影响的行数
     */
    @Update("UPDATE t_user SET last_login_time = #{lastLoginTime} WHERE id = #{userId}")
    int updateLastLoginTime(@Param("userId") Long userId, @Param("lastLoginTime") java.time.LocalDateTime lastLoginTime);

    /**
     * 统计用户总数
     * @return 用户总数
     */
    @Select("SELECT COUNT(1) FROM t_user WHERE is_deleted = 0")
    long countUsers();

    /**
     * 统计活跃用户数（状态为正常的用户）
     * @return 活跃用户数
     */
    @Select("SELECT COUNT(1) FROM t_user WHERE status = 1 AND is_deleted = 0")
    long countActiveUsers();
}