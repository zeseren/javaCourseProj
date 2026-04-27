package org.test.util;

/**
 * 页面输出转义工具。
 *
 * 用户输入的标题、选项、用户名会重新显示在 JSP 页面中。
 * 输出前做 HTML 转义，可以避免特殊字符破坏页面结构。
 */
public class HtmlUtil {
    private HtmlUtil() {
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
