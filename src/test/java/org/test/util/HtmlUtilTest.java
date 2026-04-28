package org.test.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HtmlUtil 单元测试。
 *
 * escape() 是纯函数，不依赖外部资源，直接用 JUnit 验证即可。
 */
class HtmlUtilTest {

    @Test
    void shouldReturnEmptyStringForNull() {
        assertEquals("", HtmlUtil.escape(null));
    }

    @Test
    void shouldReturnSameStringWhenNoSpecialChars() {
        String input = "Hello World 你好世界";
        assertEquals(input, HtmlUtil.escape(input));
    }

    @Test
    void shouldEscapeAmpersand() {
        assertEquals("A &amp; B", HtmlUtil.escape("A & B"));
    }

    @Test
    void shouldEscapeLessThan() {
        assertEquals("a &lt; b", HtmlUtil.escape("a < b"));
    }

    @Test
    void shouldEscapeGreaterThan() {
        assertEquals("a &gt; b", HtmlUtil.escape("a > b"));
    }

    @Test
    void shouldEscapeDoubleQuote() {
        assertEquals("&quot;hello&quot;", HtmlUtil.escape("\"hello\""));
    }

    @Test
    void shouldEscapeSingleQuote() {
        assertEquals("it&#39;s", HtmlUtil.escape("it's"));
    }

    @Test
    void shouldEscapeMultipleSpecialCharsTogether() {
        // 多个特殊字符同时出现时，每一个都要被转义
        String input = "<script>alert(\"XSS & 'attack'\")</script>";
        String expected = "&lt;script&gt;alert(&quot;XSS &amp; &#39;attack&#39;&quot;)&lt;/script&gt;";
        assertEquals(expected, HtmlUtil.escape(input));
    }

    @Test
    void shouldHandleEmptyString() {
        assertEquals("", HtmlUtil.escape(""));
    }

    @Test
    void shouldEscapeNumbersAndSymbolsCorrectly() {
        // 数字和普通标点不需要转义
        assertEquals("123, 456.78!?", HtmlUtil.escape("123, 456.78!?"));
    }

    @Test
    void shouldEscapeChineseWithSpecialChars() {
        // 中文 + 特殊字符的混合场景
        String input = "价格 < 100 & 数量 > 5";
        String expected = "价格 &lt; 100 &amp; 数量 &gt; 5";
        assertEquals(expected, HtmlUtil.escape(input));
    }
}
