package com.testplatform.service.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 执行器
 *
 * 设计为可被替换的 Spring Bean，便于在 ExecutionEngineTest 中注入桩实现。
 * 对于网络异常 / 构造异常，**不抛出**，统一以 ExecuteResult 返回
 * （statusCode=0、success=false、errorMessage 填充原因）。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
@Component
public class HttpExecutor {

    /**
     * 单次 HTTP 调用的执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteResult {
        /** HTTP 状态码；网络异常或构造失败时为 0 */
        private int statusCode;
        /** 响应头（最后一个值取值；保持插入顺序） */
        private Map<String, String> responseHeaders;
        /** 响应体原始字符串（可能为空） */
        private String responseBody;
        /** 耗时（毫秒） */
        private long elapsedMs;
        /** true：成功收到任意 HTTP 响应；false：网络异常 / 构造失败 */
        private boolean success;
        /** 错误信息（success=true 时为 null） */
        private String errorMessage;
    }

    /**
     * 发起一次 HTTP 调用。
     *
     * @param method    HTTP 方法（已 toUpperCase；为空时按 GET 处理）
     * @param url       完整 URL（已完成变量替换 + baseUrl 拼接）
     * @param headers   合并后的请求头（已完成变量替换 + Content-Type 兜底）
     * @param body      请求体原始字符串；null/空表示无 body
     * @param timeoutMs 单步超时（毫秒）；调用方保证范围合法
     */
    public ExecuteResult execute(String method, String url,
                                 Map<String, String> headers, String body,
                                 int timeoutMs) {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        String httpMethod = method == null || method.isEmpty() ? "GET" : method.toUpperCase();

        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    reqBuilder.header(e.getKey(), e.getValue());
                }
            }
        }
        RequestBody requestBody = null;
        if (StringUtils.hasText(body)) {
            String ct = HeaderMerger.getHeaderIgnoreCase(headers, "Content-Type");
            MediaType mediaType = ct != null ? MediaType.parse(ct) : null;
            requestBody = RequestBody.create(body, mediaType);
        }
        try {
            reqBuilder.method(httpMethod, requestBody);
        } catch (IllegalArgumentException ex) {
            // OkHttp 对 GET 携带 body 等非法组合会抛异常
            return ExecuteResult.builder()
                    .statusCode(0).elapsedMs(0L)
                    .success(false)
                    .errorMessage("构造请求失败：" + ex.getMessage())
                    .build();
        }

        long start = System.currentTimeMillis();
        try (Response response = client.newCall(reqBuilder.build()).execute()) {
            long elapsed = System.currentTimeMillis() - start;
            Map<String, String> respHeaders = new LinkedHashMap<>();
            for (String name : response.headers().names()) {
                respHeaders.put(name, response.header(name));
            }
            String respBody = response.body() != null ? response.body().string() : "";
            return ExecuteResult.builder()
                    .statusCode(response.code())
                    .elapsedMs(elapsed)
                    .responseHeaders(respHeaders)
                    .responseBody(respBody)
                    .success(true)
                    .errorMessage(null)
                    .build();
        } catch (IOException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("M3 步骤网络异常 url={} err={}", url, ex.getMessage());
            return ExecuteResult.builder()
                    .statusCode(0)
                    .elapsedMs(elapsed)
                    .success(false)
                    .errorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                    .build();
        }
    }
}
