package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.Assertion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 断言规则数据访问接口
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Mapper
public interface AssertionMapper extends BaseMapper<Assertion> {

    /**
     * 批量查询多个步骤下的全部断言规则
     * 安全：通过 MyBatis foreach + #{} 绑定，无注入风险
     */
    @Select("<script>" +
            "SELECT * FROM t_assertion WHERE step_id IN " +
            "<foreach collection='stepIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Assertion> selectListByStepIds(@Param("stepIds") List<Long> stepIds);

    /**
     * 物理删除多个步骤下的全部断言规则
     */
    @Delete("<script>" +
            "DELETE FROM t_assertion WHERE step_id IN " +
            "<foreach collection='stepIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteByStepIds(@Param("stepIds") List<Long> stepIds);
}
