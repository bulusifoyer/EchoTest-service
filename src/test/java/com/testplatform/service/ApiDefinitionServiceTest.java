package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.dto.ApiDefinitionAddDTO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.impl.ApiDefinitionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ApiDefinitionService 主路径单元测试
 * 采用纯 Mockito（不启动 Spring 容器），保证不依赖本地 MySQL
 *
 * 验收测试点：add 时项目不归属当前用户 → 返回"项目不存在或无权限操作"，DB 不应发生 insert
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@ExtendWith(MockitoExtension.class)
public class ApiDefinitionServiceTest {

    @Mock
    private ApiDefinitionMapper apiDefinitionMapper;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ApiDefinitionServiceImpl apiDefinitionService;

    @AfterEach
    void clearContext() {
        // 防止污染其他测试线程
        UserContext.clear();
    }

    /**
     * 数据隔离：add 时用 userB 登录，但 projectId 属于 userA
     * 期望：返回"项目不存在或无权限操作"，且 insert 不被调用
     */
    @Test
    void testAdd_DataIsolation_RejectCrossUserProject() {
        // 模拟用户 B 登录
        UserContext.setCurrentUserId(2L);

        // projectMapper 在用 (projectId=100, createBy=2) 查询时返回 null —— 表示项目不属于用户 B
        when(projectMapper.selectByIdAndCreateBy(anyLong(), anyLong())).thenReturn(null);

        ApiDefinitionAddDTO dto = new ApiDefinitionAddDTO();
        dto.setProjectId(100L);
        dto.setName("demo");
        dto.setMethod("GET");
        dto.setPath("/users");

        Result<Long> result = apiDefinitionService.add(dto);

        assertNotNull(result);
        assertEquals(Integer.valueOf(500), result.getCode());
        assertEquals("项目不存在或无权限操作", result.getMessage());
        // 关键：insert 不应被调用
        verify(apiDefinitionMapper, never()).insert(any());
    }
}
