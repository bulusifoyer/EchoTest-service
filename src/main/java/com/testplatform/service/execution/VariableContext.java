package com.testplatform.service.execution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用例执行上下文（变量字典）
 *
 * 仅做单层 ${var} 替换，不递归；未命中变量时保持原文。
 * 仅替换 URL / headers / body，不替换 stepName 等纯展示字段（由调用方决定）。
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
public final class VariableContext {

    /**
     * 变量占位符匹配 ${var}，var 仅允许字母数字下划线
     */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");

    private final Map<String, String> ctx = new LinkedHashMap<>();

    /**
     * 写入变量；value 为 null 时不写入（与"提取失败不写入"约定一致）
     */
    public void put(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        ctx.put(name, value);
    }

    /**
     * 当前已知变量数量（测试与日志使用）
     */
    public int size() {
        return ctx.size();
    }

    /**
     * 当前上下文快照（不可修改返回值）
     */
    public Map<String, String> snapshot() {
        return new LinkedHashMap<>(ctx);
    }

    /**
     * 在 text 中替换所有 ${var}：
     *   - 命中 → 用变量值替换
     *   - 未命中 → 保持原 ${var} 字面量
     *   - 不递归：替换后产生的新 ${...} 不再二次替换
     *
     * @param text 任意字符串（null 时返回 null）
     * @return 替换后的字符串
     */
    public String replace(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = VAR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String val = ctx.get(key);
            // 未命中保持原文（用 m.group(0) = "${key}"）
            String replacement = val != null ? val : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
