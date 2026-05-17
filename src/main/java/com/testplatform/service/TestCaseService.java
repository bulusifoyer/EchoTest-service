package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.dto.TestCaseDetailVO;
import com.testplatform.entity.dto.TestCaseSaveDTO;

import java.util.List;

/**
 * 测试用例业务接口
 * 用例 + 步骤 + 变量提取 + 断言 在 M2 一并管理（聚合服务）
 * 不单独开 Step / Extract / Assertion 服务，前端通过用例聚合接口完成全部配置
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
public interface TestCaseService {

    /**
     * 一次性创建：用例 + 所有步骤 + 每步的变量提取与断言
     *
     * @return 新建用例的ID
     */
    Result<Long> add(TestCaseSaveDTO dto);

    /**
     * 查询用例详情，返回完整的用例树
     */
    Result<TestCaseDetailVO> getDetail(Long id);

    /**
     * 查询某项目下当前用户的所有用例（轻量列表，不含步骤树）
     */
    Result<List<TestCase>> listByProjectId(Long projectId);

    /**
     * 全量替换：删除旧步骤/变量/断言后，按新数据重新写入
     */
    Result<Void> update(Long id, TestCaseSaveDTO dto);

    /**
     * 删除用例
     * 用例软删，子表（步骤/变量/断言）物理删除以避免遗留孤儿数据
     */
    Result<Void> delete(Long id);
}
