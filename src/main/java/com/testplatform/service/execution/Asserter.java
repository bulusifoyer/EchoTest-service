package com.testplatform.service.execution;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.testplatform.entity.Assertion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 断言判定器
 *
 * MVP 支持两类断言：
 *   - STATUS_CODE：以 HTTP 状态码为左值；expression 可为空
 *   - JSON_PATH ：以 JsonPath.read(body, expression) 为左值；expression 必填
 *
 * 三种 operator：
 *   - EQUALS      ：左右 toString 相等
 *   - CONTAINS    ：左值字符串包含右值
 *   - GREATER_THAN：双方均能转 double 时数值比较；任一无法转数字 → 断言失败 + 给出原因
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
@Slf4j
public class Asserter {

    /**
     * 单条断言结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssertResult {
        /** 断言类型 */
        private String type;
        /** 表达式（STATUS_CODE 时为空） */
        private String expression;
        /** 比较符 */
        private String operator;
        /** 期望值 */
        private String expected;
        /** 实际值（左值） */
        private String actual;
        /** 是否通过 */
        private boolean passed;
        /** 描述（失败原因 / 通过描述） */
        private String message;
    }

    /**
     * 对一个步骤的所有断言进行判定，返回每条结果。
     * 任一条不通过 → 整体失败（由调用方汇总）。
     *
     * @param assertions  断言列表（可为 null/空 → 视为通过零条；调用方决定语义）
     * @param statusCode  本步 HTTP 状态码（网络异常时为 0）
     * @param responseBody 响应体（可为 null/空字符串）
     * @return 结果列表（保持入参顺序；空入参返回空列表）
     */
    public List<AssertResult> assertAll(List<Assertion> assertions, int statusCode, String responseBody) {
        List<AssertResult> results = new ArrayList<>();
        if (assertions == null || assertions.isEmpty()) {
            return results;
        }
        for (Assertion a : assertions) {
            results.add(assertOne(a, statusCode, responseBody));
        }
        return results;
    }

    /**
     * 单条断言判定
     */
    public AssertResult assertOne(Assertion a, int statusCode, String responseBody) {
        String type = a.getAssertType() == null ? "" : a.getAssertType().trim().toUpperCase();
        String op = a.getOperator() == null || a.getOperator().isEmpty()
                ? "EQUALS" : a.getOperator().trim().toUpperCase();
        String expected = a.getExpectedValue() == null ? "" : a.getExpectedValue();
        String expression = a.getExpression() == null ? "" : a.getExpression();

        AssertResult.AssertResultBuilder b = AssertResult.builder()
                .type(type).expression(expression).operator(op).expected(expected);

        // 1. 取左值
        String actual;
        switch (type) {
            case "STATUS_CODE":
                actual = String.valueOf(statusCode);
                break;
            case "JSON_PATH":
                if (!StringUtils.hasText(expression)) {
                    return b.actual(null).passed(false)
                            .message("JSON_PATH 断言缺少表达式").build();
                }
                if (!StringUtils.hasText(responseBody)) {
                    return b.actual(null).passed(false)
                            .message("响应体为空，无法 JsonPath 取值").build();
                }
                try {
                    Object raw = JsonPath.read(responseBody, expression);
                    actual = raw == null ? "null" : raw.toString();
                } catch (PathNotFoundException pnfe) {
                    return b.actual(null).passed(false)
                            .message("JsonPath 未命中：" + expression).build();
                } catch (Exception ex) {
                    return b.actual(null).passed(false)
                            .message("JsonPath 解析异常：" + ex.getMessage()).build();
                }
                break;
            default:
                return b.actual(null).passed(false)
                        .message("不支持的断言类型：" + type).build();
        }

        // 2. 比较
        boolean passed;
        String msg;
        switch (op) {
            case "EQUALS":
                passed = expected.equals(actual);
                msg = passed ? "等于期望值" : "实际值『" + actual + "』≠ 期望值『" + expected + "』";
                break;
            case "CONTAINS":
                passed = actual != null && actual.contains(expected);
                msg = passed ? "包含期望值" : "实际值『" + actual + "』未包含『" + expected + "』";
                break;
            case "GREATER_THAN":
                try {
                    double l = Double.parseDouble(actual);
                    double r = Double.parseDouble(expected);
                    passed = l > r;
                    msg = passed ? l + " > " + r
                            : "实际值『" + actual + "』未大于期望值『" + expected + "』";
                } catch (NumberFormatException nfe) {
                    passed = false;
                    msg = "GREATER_THAN 需要数值，但实际值『" + actual + "』或期望值『" + expected + "』非数字";
                }
                break;
            default:
                passed = false;
                msg = "不支持的比较运算符：" + op;
        }
        return b.actual(actual).passed(passed).message(msg).build();
    }
}
