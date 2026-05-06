package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 t_user，存储系统用户信息
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")  // 指定数据库表名
public class User {

    /**
     * 主键ID
     * 自增主键，唯一标识一个用户
     */
    @TableId(type = IdType.AUTO)  // 指定主键生成策略为自增
    private Long id;

    /**
     * 登录账号
     * 用户登录时使用的账号，全局唯一
     */
    private String username;

    /**
     * 登录密码
     * 存储加密后的密码，使用BCrypt算法加密
     */
    private String password;

    /**
     * 用户昵称/展示名
     * 用户在系统中显示的名称
     */
    private String nickname;

    /**
     * 邮箱地址
     * 用户的电子邮箱，可用于找回密码等功能
     */
    private String email;

    /**
     * 用户头像URL
     * 存储用户头像图片的访问地址
     */
    private String avatar;

    /**
     * 账号状态
     * 1: 正常（可以正常登录使用）
     * 0: 停用（禁止登录）
     */
    private Integer status;

    /**
     * 最后登录时间
     * 记录用户最后一次成功登录的时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 注册时间
     * 用户账号创建时间，自动填充
     */
    @TableField(fill = FieldFill.INSERT)  // 插入时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录用户信息最后修改时间，自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时自动填充
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     * 0: 正常（数据有效）
     * 1: 删除（数据已删除，但仍在数据库中保留）
     */
    @TableLogic  // 逻辑删除注解
    private Integer isDeleted;

    /**
     * 获取用户显示名称
     * 如果昵称不为空则返回昵称，否则返回用户名
     * @return 用户显示名称
     */
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }

    /**
     * 检查账号是否有效
     * @return 账号是否处于正常状态
     */
    public boolean isAccountActive() {
        return status != null && status == 1 && (isDeleted == null || isDeleted == 0);
    }

    /**
     * 更新最后登录时间
     * 在用户登录成功后调用
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }
}