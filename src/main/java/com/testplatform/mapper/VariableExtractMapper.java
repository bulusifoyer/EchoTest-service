package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.VariableExtract;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 变量提取规则数据访问接口
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Mapper
public interface VariableExtractMapper extends BaseMapper<VariableExtract> {

    /**
     * 批量查询多个步骤下的全部变量提取规则
     * 使用场景：获取用例详情时，一次性把当前用例所有步骤的提取规则查回，避免 N+1
     *
     * 安全：通过 MyBatis foreach + #{} 绑定，非字符串拼接，无注入风险
     */
    @Select("<script>" +
            "SELECT * FROM t_variable_extract WHERE step_id IN " +
            "<foreach collection='stepIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<VariableExtract> selectListByStepIds(@Param("stepIds") List<Long> stepIds);

    /**
     * 物理删除多个步骤下的全部变量提取规则
     * 在用例 update（全量替换）与 delete 时使用
     */
    @Delete("<script>" +
            "DELETE FROM t_variable_extract WHERE step_id IN " +
            "<foreach collection='stepIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteByStepIds(@Param("stepIds") List<Long> stepIds);
}
