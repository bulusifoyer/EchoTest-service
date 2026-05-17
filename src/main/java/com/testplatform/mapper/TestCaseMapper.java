package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 测试用例数据访问接口
 * 继承 BaseMapper 并提供基于 create_by 的数据隔离查询
 *
 * 安全说明：所有动态条件均通过 #{} 参数绑定，无 SQL 注入风险
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {

    /**
     * 根据用例ID与创建人ID联合查询，用于数据隔离校验
     */
    @Select("SELECT * FROM t_test_case " +
            "WHERE id = #{id} AND create_by = #{createBy} AND is_deleted = 0")
    TestCase selectByIdAndCreateBy(@Param("id") Long id,
                                   @Param("createBy") Long createBy);

    /**
     * 查询指定项目下当前用户的所有用例
     * 按 update_time 降序排序
     */
    @Select("SELECT * FROM t_test_case " +
            "WHERE project_id = #{projectId} AND create_by = #{createBy} AND is_deleted = 0 " +
            "ORDER BY update_time DESC")
    List<TestCase> selectListByProjectAndCreateBy(@Param("projectId") Long projectId,
                                                  @Param("createBy") Long createBy);
}
