package com.testplatform.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 接口试调结果视图对象
 * 返回一次试调的响应摘要，供前端展示
 *
 * @author 测试平台开发团队
 * @since 2026-05-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTryRunResultVO {

    /**
     * HTTP 响应状态码；调用失败（网络层异常）时为 0
     */
    private Integer statusCode;

    /**
     * 请求耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 响应头集合（key 为 header 名，value 为首个取值）
     */
    private Map<String, String> responseHeaders;

    /**
     * 响应体（原始字符串）
     */
    private String responseBody;

    /**
     * 是否成功收到响应
     * true：收到任意 HTTP 状态码；false：网络异常、超时、域名解析失败等
     */
    private Boolean success;

    /**
     * 错误信息；success=true 时为 null
     */
    private String errorMessage;
}
