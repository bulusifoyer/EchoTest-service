package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.dto.ExecutionReportDetailVO;
import com.testplatform.entity.dto.ExecutionReportVO;
import com.testplatform.entity.dto.ExecutionRunDTO;

import java.util.List;

/**
 * 测试执行与报告业务接口（M3）
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
public interface ExecutionService {

    /**
     * 同步执行用例（串行 + 遇错即停），返回新建的报告 ID。
     */
    Result<Long> run(ExecutionRunDTO dto);

    /**
     * 报告详情（含明细列表）。
     */
    Result<ExecutionReportDetailVO> getReportDetail(Long reportId);

    /**
     * 项目下当前用户的报告列表（按创建时间倒序）。
     */
    Result<List<ExecutionReportVO>> listByProject(Long projectId);

    /**
     * 软删报告（明细不物理删除，仍可通过该 reportId 查询，但前端列表过滤掉 is_deleted=1）。
     */
    Result<Void> deleteReport(Long reportId);
}
