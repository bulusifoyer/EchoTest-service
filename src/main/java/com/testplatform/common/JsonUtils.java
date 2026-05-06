package com.testplatform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * JSON 工具类
 * 提供 JSON 字符串的格式验证与解析功能
 * 主要用于校验前端传入的 global_headers 字段是否为合法的 JSON 格式
 *
 * @author 测试平台开发团队
 * @since 2024-04-22
 */
@Slf4j
@Component
public class JsonUtils {

    private static ObjectMapper objectMapper;

    /**
     * 初始化 ObjectMapper 实例
     * Spring 容器启动后自动执行
     */
    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 验证字符串是否为合法的 JSON 格式
     * 支持对象（{}）和数组（[]）两种格式的校验
     *
     * @param jsonStr 待验证的字符串
     * @return true: 合法JSON, false: 非法JSON 或 null/空字符串
     */
    public static boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            // 空值视为合法，因为 global_headers 字段为选填项
            return true;
        }

        String trimmed = jsonStr.trim();
        // 快速检查：JSON 必须以 { 或 [ 开头，以 } 或 ] 结尾
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))
                && !(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return false;
        }

        try {
            // 使用 Jackson 进行严格解析
            objectMapper.readTree(trimmed);
            return true;
        } catch (JsonProcessingException e) {
            log.debug("JSON 格式验证失败，非法字符串: {}", jsonStr, e);
            return false;
        }
    }

    /**
     * 验证字符串是否为合法的 JSON 对象格式（必须以 {} 包裹）
     * 适用于 global_headers 字段，要求必须是 JSON 对象
     *
     * @param jsonStr 待验证的字符串
     * @return true: 合法JSON对象, false: 非法JSON对象 或 null/空字符串
     */
    public static boolean isValidJsonObject(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            // 空值视为合法，因为 global_headers 字段为选填项
            return true;
        }

        String trimmed = jsonStr.trim();
        // 必须是以 {} 包裹的 JSON 对象
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }

        try {
            objectMapper.readTree(trimmed);
            return true;
        } catch (JsonProcessingException e) {
            log.debug("JSON 对象格式验证失败，非法字符串: {}", jsonStr, e);
            return false;
        }
    }

    /**
     * 格式化 JSON 字符串，使其具有标准的缩进和换行
     * 主要用于日志输出和调试
     *
     * @param jsonStr 原始 JSON 字符串
     * @return 格式化后的 JSON 字符串，若解析失败则返回原字符串
     */
    public static String formatJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return jsonStr;
        }

        try {
            Object jsonNode = objectMapper.readValue(jsonStr.trim(), Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            log.warn("JSON 格式化失败，返回原字符串", e);
            return jsonStr;
        }
    }
}