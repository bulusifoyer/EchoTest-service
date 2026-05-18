package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.ExecutionDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 执行明细数据访问接口
 *
 * 安全：明细本身无 create_by 列；归属由父报告承载。所有查询都通过 reportId 进入，
 * 调用方必须先通过 {@link ExecutionReportMapper#selectByIdAndCreateBy} 校验报告归属。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Mapper
public interface ExecutionDetailMapper extends BaseMapper<ExecutionDetail> {

    /**
     * 按报告 ID 查询全部明细，按 step_order 升序。
     */
    @Select("SELECT * FROM t_execution_detail " +
            "WHERE report_id = #{reportId} ORDER BY step_order ASC, id ASC")
    List<ExecutionDetail> selectListByReportIdOrderByStepOrder(@Param("reportId") Long reportId);
}
