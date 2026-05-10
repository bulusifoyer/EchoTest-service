package com.testplatform.service;

import com.sun.net.httpserver.HttpServer;
import com.testplatform.common.Result;
import com.testplatform.common.UserContext;
import com.testplatform.entity.ApiDefinition;
import com.testplatform.entity.Environment;
import com.testplatform.entity.Project;
import com.testplatform.entity.dto.ApiTryRunDTO;
import com.testplatform.entity.dto.ApiTryRunResultVO;
import com.testplatform.mapper.ApiDefinitionMapper;
import com.testplatform.mapper.EnvironmentMapper;
import com.testplatform.mapper.ProjectMapper;
import com.testplatform.service.impl.ApiTryRunServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * ApiTryRunService 主路径单元测试
 * 通过 JDK 内置的 HttpServer（零额外依赖）起一个本地 HTTP 桩，
 * 验证 headers 合并优先级：env.global → api.requestHeaders → dto.overrideHeaders（高覆低）
 * 以及 body 非空时自动补 Content-Type
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@ExtendWith(MockitoExtension.class)
public class ApiTryRunServiceTest {

    @Mock
    private ApiDefinitionMapper apiDefinitionMapper;

    @Mock
    private EnvironmentMapper environmentMapper;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ApiTryRunServiceImpl apiTryRunService;

    private HttpServer httpServer;
    private int port;

    /** 捕获桩服务实际收到的 headers，用于断言合并结果 */
    private final AtomicReference<Map<String, List<String>>> capturedHeaders = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        // 绑定随机端口，避免与其他进程冲突
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = httpServer.getAddress().getPort();
        httpServer.createContext("/", exchange -> {
            // 记录本次请求的 headers
            capturedHeaders.set(exchange.getRequestHeaders());
            byte[] body = "OK".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();
    }

    @AfterEach
    void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        UserContext.clear();
    }

    @Test
    void testTryRun_MergeHeaders_OverrideTakesPriority_AndAutoFillContentType() {
        UserContext.setCurrentUserId(1L);

        // 1. Mock 接口定义
        ApiDefinition api = ApiDefinition.builder()
                .id(10L)
                .projectId(100L)
                .createBy(1L)
                .name("demo")
                .method("POST")
                .path("/test")
                .requestHeaders("{\"X-Layer\":\"api\",\"X-From-Api\":\"api\"}")
                .requestBody("{\"k\":\"v\"}") // 非空 body，触发自动补 Content-Type
                .build();
        when(apiDefinitionMapper.selectByIdAndCreateBy(10L, 1L)).thenReturn(api);

        // 2. Mock 环境（指向本地 HttpServer）
        Environment env = Environment.builder()
                .id(20L)
                .projectId(100L)
                .envName("test")
                .baseUrl("http://127.0.0.1:" + port)
                .globalHeaders("{\"X-Layer\":\"env\",\"X-From-Env\":\"env\"}")
                .build();
        when(environmentMapper.selectById(20L)).thenReturn(env);

        // 3. Mock 项目归属校验
        Project project = Project.builder().id(100L).createBy(1L).name("P1").build();
        when(projectMapper.selectByIdAndCreateBy(100L, 1L)).thenReturn(project);

        // 4. 准备 DTO（override 层：覆盖 X-Layer）
        ApiTryRunDTO dto = new ApiTryRunDTO();
        dto.setApiId(10L);
        dto.setEnvId(20L);
        dto.setOverrideHeaders("{\"X-Layer\":\"override\",\"X-From-Override\":\"override\"}");
        dto.setTimeoutMs(5000);

        // 5. 执行
        Result<ApiTryRunResultVO> result = apiTryRunService.tryRun(dto);

        // 6. 断言 Service 返回
        assertNotNull(result);
        assertEquals(Integer.valueOf(200), result.getCode());
        ApiTryRunResultVO vo = result.getData();
        assertNotNull(vo);
        assertTrue(vo.getSuccess(), "试调应成功");
        assertEquals(Integer.valueOf(200), vo.getStatusCode());
        assertEquals("OK", vo.getResponseBody());

        // 7. 断言桩服务实际收到的 headers 合并结果
        // com.sun.net.httpserver.Headers 以"首字母大写+后续小写"的规范 key 存值，故 X-Layer 规范化为 X-layer
        Map<String, List<String>> received = capturedHeaders.get();
        assertNotNull(received, "桩服务应收到请求");
        // X-Layer 被 override 覆盖
        assertEquals("override", firstHeader(received, "X-Layer"));
        // env 独有的 header 保留
        assertEquals("env", firstHeader(received, "X-From-Env"));
        // api 独有的 header 保留
        assertEquals("api", firstHeader(received, "X-From-Api"));
        // override 独有的 header 保留
        assertEquals("override", firstHeader(received, "X-From-Override"));
        // body 非空且未显式设置 Content-Type，应自动补为 application/json
        String contentType = firstHeader(received, "Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.toLowerCase().contains("application/json"));
    }

    /** 大小写不敏感地从 HTTP headers 里取首个值（JDK 8 兼容写法） */
    private static String firstHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                List<String> values = e.getValue();
                return (values == null || values.isEmpty()) ? null : values.get(0);
            }
        }
        return null;
    }
}
