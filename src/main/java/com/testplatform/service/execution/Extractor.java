package com.testplatform.service.execution;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.testplatform.entity.VariableExtract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量提取器
 *
 * 提取规则：从响应体按 JsonPath 取值，写入 {@link VariableContext}。
 *
 * 失败处理（按约束 11）：
 *   - JsonPath 解析失败 / 未命中 → 不抛出，不写入上下文（视为该变量不存在）
 *   - 仅记日志 + 在 detail.assertResult 中以"变量提取失败"信息保留，便于答辩排查
 *   - 后续断言/步骤如果引用该变量，会因 ${var} 未替换或断言不通过而自然失败
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
public class Extractor {

    /**
     * 单条提取结果（用于落库 detail.assertResult，便于排查）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractResult {
        private String variableName;
        private String jsonPath;
        /** 提取到的值（成功时） */
        private String value;
        /** 是否成功 */
        private boolean success;
        /** 失败原因（成功时为 null） */
        private String message;
    }

    /**
     * 对一个步骤的所有变量提取规则进行处理，命中的写入 ctx。
     *
     * @param extracts     提取规则列表（可为 null/空）
     * @param responseBody 响应体（可为 null/空）
     * @param ctx          变量上下文（写入目标）
     * @return 每条规则的结果（保持入参顺序）
     */
    public List<ExtractResult> extractAll(List<VariableExtract> extracts,
                                          String responseBody,
                                          VariableContext ctx) {
        List<ExtractResult> results = new ArrayList<>();
        if (extracts == null || extracts.isEmpty()) {
            return results;
        }
        for (VariableExtract ve : extracts) {
            results.add(extractOne(ve, responseBody, ctx));
        }
        return results;
    }

    private ExtractResult extractOne(VariableExtract ve, String responseBody, VariableContext ctx) {
        String name = ve.getVariableName();
        String path = ve.getJsonPath();
        ExtractResult.ExtractResultBuilder b = ExtractResult.builder()
                .variableName(name).jsonPath(path);

        if (!StringUtils.hasText(name) || !StringUtils.hasText(path)) {
            return b.success(false).message("变量名或 JsonPath 为空，跳过").build();
        }
        if (!StringUtils.hasText(responseBody)) {
            return b.success(false).message("响应体为空，无法提取").build();
        }
        try {
            Object raw = JsonPath.read(responseBody, path);
            if (raw == null) {
                return b.success(false).message("JsonPath 命中但值为 null，未写入上下文").build();
            }
            String val = raw.toString();
            ctx.put(name, val);
            return b.success(true).value(val).message("提取成功").build();
        } catch (PathNotFoundException pnfe) {
            log.debug("变量提取未命中：{} = {}", name, path);
            return b.success(false).message("JsonPath 未命中：" + path).build();
        } catch (Exception ex) {
            log.warn("变量提取异常：{} = {}，err={}", name, path, ex.getMessage());
            return b.success(false).message("JsonPath 解析异常：" + ex.getMessage()).build();
        }
    }
}
