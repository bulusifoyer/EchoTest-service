package com.testplatform.service.execution;

import com.testplatform.entity.Assertion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserter 单元测试
 *
 * 覆盖：
 *   1. STATUS_CODE × EQUALS 通过
 *   2. STATUS_CODE × EQUALS 不通过
 *   3. JSON_PATH × EQUALS 通过
 *   4. JSON_PATH × CONTAINS 通过 / 不通过
 *   5. JSON_PATH 路径未命中 → 失败
 *   6. JSON_PATH × GREATER_THAN 通过 + 非数值时失败
 *   7. expression 缺失 / 响应体空 → 显式失败
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
public class AsserterTest {

    private final Asserter asserter = new Asserter();

    @Test
    void statusCodeEqualsPasses() {
        Assertion a = Assertion.builder()
                .assertType("STATUS_CODE").operator("EQUALS").expectedValue("200").build();
        Asserter.AssertResult r = asserter.assertOne(a, 200, "{}");
        assertTrue(r.isPassed());
        assertEquals("200", r.getActual());
    }

    @Test
    void statusCodeEqualsFails() {
        Assertion a = Assertion.builder()
                .assertType("STATUS_CODE").operator("EQUALS").expectedValue("200").build();
        Asserter.AssertResult r = asserter.assertOne(a, 500, "{}");
        assertFalse(r.isPassed());
    }

    @Test
    void jsonPathEqualsPasses() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("EQUALS")
                .expression("$.code").expectedValue("0").build();
        Asserter.AssertResult r = asserter.assertOne(a, 200, "{\"code\":0,\"data\":\"x\"}");
        assertTrue(r.isPassed());
    }

    @Test
    void jsonPathContainsBranches() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("CONTAINS")
                .expression("$.msg").expectedValue("ok").build();
        assertTrue(asserter.assertOne(a, 200, "{\"msg\":\"all is ok now\"}").isPassed());
        assertFalse(asserter.assertOne(a, 200, "{\"msg\":\"failed\"}").isPassed());
    }

    @Test
    void jsonPathNotFoundFails() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("EQUALS")
                .expression("$.missing").expectedValue("x").build();
        Asserter.AssertResult r = asserter.assertOne(a, 200, "{\"a\":1}");
        assertFalse(r.isPassed());
    }

    @Test
    void greaterThanWorksAndFailsOnNonNumeric() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("GREATER_THAN")
                .expression("$.score").expectedValue("60").build();
        assertTrue(asserter.assertOne(a, 200, "{\"score\":80}").isPassed());
        assertFalse(asserter.assertOne(a, 200, "{\"score\":50}").isPassed());

        // 非数值字符串 → 失败 + 给出原因
        Asserter.AssertResult r = asserter.assertOne(a, 200, "{\"score\":\"high\"}");
        assertFalse(r.isPassed());
        assertTrue(r.getMessage() != null && r.getMessage().contains("非数字"));
    }

    @Test
    void jsonPathMissingExpressionExplicitFail() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("EQUALS")
                .expression("").expectedValue("x").build();
        Asserter.AssertResult r = asserter.assertOne(a, 200, "{}");
        assertFalse(r.isPassed());
    }

    @Test
    void jsonPathEmptyBodyExplicitFail() {
        Assertion a = Assertion.builder()
                .assertType("JSON_PATH").operator("EQUALS")
                .expression("$.x").expectedValue("y").build();
        Asserter.AssertResult r = asserter.assertOne(a, 200, "");
        assertFalse(r.isPassed());
    }
}
