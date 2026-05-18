package com.testplatform.service.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 请求头合并工具
 *
 * 由 M1 ApiTryRunServiceImpl 抽出，供 M3 执行引擎与单接口试调共用，避免两边算法分裂。
 *
 * 合并优先级：传入数组靠后的一层覆盖靠前的一层（高 → 覆盖 → 低）。
 * 比较策略：key 比较忽略大小写，key 大小写以"高优先级层"为准。
 * 空字符串 / null 层会被跳过；返回 LinkedHashMap 保留插入顺序。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
public final class HeaderMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HeaderMerger() {
    }

    /**
     * 把多层 JSON 字符串 headers 合并为 Map（顺序：低优先级 → 高优先级，后者覆盖前者）。
     *
     * @param layersFromLowToHigh 每层为 JSON 对象字符串，可为 null 或空字符串（跳过）
     * @return 合并后的 LinkedHashMap
     * @throws IOException 任一非空层不是合法 JSON 对象时抛出
     */
    public static Map<String, String> mergeJsonHeaders(String... layersFromLowToHigh) throws IOException {
        Map<String, String> merged = new LinkedHashMap<>();
        if (layersFromLowToHigh == null) {
            return merged;
        }
        for (String layer : layersFromLowToHigh) {
            if (!StringUtils.hasText(layer)) {
                continue;
            }
            // 用 TreeMap 反序列化以接受 key 任意大小写；后续合并到 LinkedHashMap 保留顺序
            Map<String, String> one = MAPPER.readValue(
                    layer, new TypeReference<TreeMap<String, String>>() {});
            for (Map.Entry<String, String> e : one.entrySet()) {
                removeHeaderIgnoreCase(merged, e.getKey());
                merged.put(e.getKey(), e.getValue());
            }
        }
        return merged;
    }

    /**
     * Map 中是否存在指定 header（key 忽略大小写）。
     */
    public static boolean containsHeaderIgnoreCase(Map<String, String> headers, String name) {
        return getHeaderIgnoreCase(headers, name) != null;
    }

    /**
     * 取值（key 忽略大小写）；未命中返回 null。
     */
    public static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (name.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return null;
    }

    /**
     * 移除（key 忽略大小写）；不存在则忽略。
     */
    public static void removeHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) return;
        headers.entrySet().removeIf(e -> name.equalsIgnoreCase(e.getKey()));
    }
}
