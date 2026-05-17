package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.TestStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 测试步骤数据访问接口
 * 步骤是用例的子表，归属隔离通过父用例传递；不做软删
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Mapper
public interface TestStepMapper extends BaseMapper<TestStep> {

    /**
     * 查询某用例下的所有步骤，按执行顺序升序
     */
    @Select("SELECT * FROM t_test_step WHERE case_id = #{caseId} ORDER BY step_order ASC")
    List<TestStep> selectListByCaseIdOrderByStepOrder(@Param("caseId") Long caseId);

    /**
     * 物理删除某用例下的全部步骤
     * 在用例 update（全量替换）与 delete 时使用
     */
    @Delete("DELETE FROM t_test_step WHERE case_id = #{caseId}")
    int deleteByCaseId(@Param("caseId") Long caseId);
}
