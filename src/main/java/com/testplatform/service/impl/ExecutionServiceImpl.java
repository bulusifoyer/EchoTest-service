package com.testplatform.service.impl;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ExecutionDetail;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.entity.dto.ExecutionDetailVO;
import com.testplatform.entity.dto.ExecutionReportDetailVO;
import com.testplatform.entity.dto.ExecutionReportVO;
import com.testplatform.entity.dto.ExecutionRunDTO;
import com.testplatform.mapper.ExecutionDetailMapper;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.execution.ExecutionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 测试执行业务实现（M3）
 *
 * 职责切分：
 *   - 本类负责"对外接口契约 + 数据隔离 + Mapper 编排"
 *   - {@link ExecutionEngine} 负责"用例执行编排 + 写报告"
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
@Service
public class ExecutionServiceImpl implements ExecutionService {

    @Autowired private ExecutionEngine executionEngine;
    @Autowired private ExecutionReportMapper reportMapper;
    @Autowired private ExecutionDetailMapper detailMapper;

    @Override
    public Result<Long> run(ExecutionRunDTO dto) {
        if (dto == null) {
            return Result.error("请求体不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }
        try {
            Long reportId = executionEngine.run(
                    dto.getCaseId(), dto.getEnvId(), dto.getTimeoutMs(), currentUserId);
            return Result.success("执行完成", reportId);
        } catch (IllegalArgumentException ex) {
            // 校验阶段失败（用例/环境/接口归属、字段缺失等）
            return Result.error(ex.getMessage());
        } catch (Exception ex) {
            log.error("M3 执行未知异常 caseId={} envId={}", dto.getCaseId(), dto.getEnvId(), ex);
            return Result.error("执行异常：" + ex.getMessage());
        }
    }

    @Override
    public Result<ExecutionReportDetailVO> getReportDetail(Long reportId) {
        if (reportId == null) {
            return Result.error("报告ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 先校验归属（防止越权）
        ExecutionReport rep = reportMapper.selectByIdAndCreateBy(reportId, currentUserId);
        if (rep == null) {
            return Result.error("报告不存在或无权限访问");
        }
        ExecutionReportVO repVO = reportMapper.selectVoByIdAndCreateBy(reportId, currentUserId);
        if (repVO == null) {
            return Result.error("报告不存在或无权限访问");
        }

        // 查明细（明细本身无 create_by，归属由父报告承载，已校验）
        List<ExecutionDetail> details = detailMapper.selectListByReportIdOrderByStepOrder(reportId);
        List<ExecutionDetailVO> detailVOs = details == null ? new ArrayList<>() :
                details.stream().map(this::toDetailVO).collect(java.util.stream.Collectors.toList());

        ExecutionReportDetailVO vo = ExecutionReportDetailVO.builder()
                .report(repVO)
                .details(detailVOs)
                .build();
        return Result.success(vo);
    }

    @Override
    public Result<List<ExecutionReportVO>> listByProject(Long projectId) {
        if (projectId == null) {
            return Result.error("项目ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }
        List<ExecutionReportVO> list = reportMapper.selectVoListByProjectAndCreateBy(projectId, currentUserId);
        return Result.success(list == null ? Collections.emptyList() : list);
    }

    @Override
    public Result<Void> deleteReport(Long reportId) {
        if (reportId == null) {
            return Result.error("报告ID不能为空");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }
        ExecutionReport rep = reportMapper.selectByIdAndCreateBy(reportId, currentUserId);
        if (rep == null) {
            return Result.error("报告不存在或无权限访问");
        }
        // 软删（@TableLogic 自动生效）
        reportMapper.deleteById(reportId);
        return Result.success();
    }

    // ---------- helpers ----------

    private ExecutionDetailVO toDetailVO(ExecutionDetail d) {
        return ExecutionDetailVO.builder()
                .id(d.getId())
                .reportId(d.getReportId())
                .stepId(d.getStepId())
                .stepOrder(d.getStepOrder())
                .requestMethod(d.getRequestMethod())
                .requestUrl(d.getRequestUrl())
                .actualRequest(d.getActualRequest())
                .actualResponse(d.getActualResponse())
                .statusCode(d.getStatusCode())
                .assertResult(d.getAssertResult())
                .failReason(d.getFailReason())
                .status(d.getStatus())
                .elapsedMs(d.getElapsedMs())
                .createTime(d.getCreateTime())
                .build();
    }
}
