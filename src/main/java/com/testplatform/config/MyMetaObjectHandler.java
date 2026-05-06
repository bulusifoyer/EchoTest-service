package com.testplatform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 负责在插入和更新操作时自动填充 create_time 和 update_time 字段
 * 所有实体类中标记了 @TableField(fill = ...) 的字段都会由此类统一处理
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Component  // 注册为 Spring Bean，MyBatis-Plus 会自动识别并调用
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作时的字段填充策略
     * 当执行 insert 或 insertOrUpdate 时，MyBatis-Plus 会自动调用此方法
     *
     * @param metaObject 当前操作的对象元数据，包含实体类的所有字段信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 获取当前时间作为填充值
        LocalDateTime now = LocalDateTime.now();

        // 严格填充：若字段值为 null 才进行填充，避免覆盖已手动设置的值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        log.debug("自动填充插入字段，类名：{}，填充时间：{}", metaObject.getOriginalObject().getClass().getName(), now);
    }

    /**
     * 更新操作时的字段填充策略
     * 当执行 update 或 updateById 时，MyBatis-Plus 会自动调用此方法
     *
     * @param metaObject 当前操作的对象元数据，包含实体类的所有字段信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 获取当前时间作为填充值
        LocalDateTime now = LocalDateTime.now();

        // 严格填充：若字段值为 null 才进行填充，避免覆盖已手动设置的值
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);

        log.debug("自动填充更新字段，类名：{}，填充时间：{}", metaObject.getOriginalObject().getClass().getName(), now);
    }
}
