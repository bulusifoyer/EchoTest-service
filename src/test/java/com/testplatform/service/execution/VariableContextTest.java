package com.testplatform.service.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * VariableContext 单元测试
 *
 * 覆盖：
 *   1. 单变量替换
 *   2. 多变量替换（同一行多次）
 *   3. 未命中保持原文
 *   4. 不递归（替换后产生的新 ${...} 不再二次替换）
 *   5. null 输入返回 null
 *
 * @author 测试平台开发团队
 * @since 2026-05-18
 */
public class VariableContextTest {

    @Test
    void singleVariableReplaced() {
        VariableContext ctx = new VariableContext();
        ctx.put("token", "abc123");
        assertEquals("Bearer abc123", ctx.replace("Bearer ${token}"));
    }

    @Test
    void multipleVariablesReplaced() {
        VariableContext ctx = new VariableContext();
        ctx.put("user", "alice");
        ctx.put("id", "42");
        assertEquals("/users/alice/profile?uid=42",
                ctx.replace("/users/${user}/profile?uid=${id}"));
    }

    @Test
    void unresolvedVariableKeepsOriginalLiteral() {
        VariableContext ctx = new VariableContext();
        ctx.put("a", "x");
        // ${b} 未命中应保持原文，${a} 正常替换
        assertEquals("x-${b}", ctx.replace("${a}-${b}"));
    }

    @Test
    void noRecursiveReplacement() {
        VariableContext ctx = new VariableContext();
        // 把 outer 的值设成另一个变量字面量 ${inner}
        ctx.put("outer", "${inner}");
        ctx.put("inner", "should-not-show");
        // 期望：outer 替换出 ${inner} 字面量，不再递归
        assertEquals("hi-${inner}", ctx.replace("hi-${outer}"));
    }

    @Test
    void nullInputReturnsNull() {
        VariableContext ctx = new VariableContext();
        assertNull(ctx.replace(null));
    }

    @Test
    void putNullValueIgnored() {
        VariableContext ctx = new VariableContext();
        ctx.put("x", null);
        assertEquals(0, ctx.size());
    }
}
