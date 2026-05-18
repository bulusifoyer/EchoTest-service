package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.entity.dto.ExecutionReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 执行报告数据访问接口
 *
 * 数据隔离：所有查询都按 create_by + is_deleted = 0 过滤；列表查询额外按 project_id 过滤。
 *
 * 安全：所有动态条件均通过 #{} 参数绑定，无 SQL 注入风险。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Mapper
public interface ExecutionReportMapper extends BaseMapper<ExecutionReport> {

    /**
     * 按 ID + 创建人查询单条报告（数据隔离）。
     */
    @Select("SELECT * FROM t_execution_report " +
            "WHERE id = #{id} AND create_by = #{createBy} AND is_deleted = 0")
    ExecutionReport selectByIdAndCreateBy(@Param("id") Long id,
                                          @Param("createBy") Long createBy);

    /**
     * 按 ID + 创建人查询单条报告 VO（含 caseName / envName）。
     */
    @Select("SELECT r.id, r.case_id, r.project_id, r.env_id, r.create_by, " +
            "       r.status, r.total_steps, r.passed_steps, r.failed_steps, " +
            "       r.start_time, r.end_time, r.total_duration_ms, " +
            "       r.executor, r.create_time, " +
            "       c.case_name AS case_name, e.env_name AS env_name " +
            "FROM t_execution_report r " +
            "LEFT JOIN t_test_case c ON c.id = r.case_id " +
            "LEFT JOIN t_environment e ON e.id = r.env_id " +
            "WHERE r.id = #{id} AND r.create_by = #{createBy} AND r.is_deleted = 0")
    ExecutionReportVO selectVoByIdAndCreateBy(@Param("id") Long id,
                                              @Param("createBy") Long createBy);

    /**
     * 项目下当前用户的报告列表（按更新时间倒序，含 caseName / envName）。
     */
    @Select("SELECT r.id, r.case_id, r.project_id, r.env_id, r.create_by, " +
            "       r.status, r.total_steps, r.passed_steps, r.failed_steps, " +
            "       r.start_time, r.end_time, r.total_duration_ms, " +
            "       r.executor, r.create_time, " +
            "       c.case_name AS case_name, e.env_name AS env_name " +
            "FROM t_execution_report r " +
            "LEFT JOIN t_test_case c ON c.id = r.case_id " +
            "LEFT JOIN t_environment e ON e.id = r.env_id " +
            "WHERE r.project_id = #{projectId} " +
            "  AND r.create_by = #{createBy} " +
            "  AND r.is_deleted = 0 " +
            "ORDER BY r.create_time DESC, r.id DESC")
    List<ExecutionReportVO> selectVoListByProjectAndCreateBy(@Param("projectId") Long projectId,
                                                             @Param("createBy") Long createBy);
}
