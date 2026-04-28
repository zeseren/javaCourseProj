package org.test.util;

/**
 * HTML 输出转义工具 —— 网页安全的"守门人"。
 *
 * 这个类存在的必要性：
 * ────────────────────
 * 假设用户在投票选项里输入了 "<script>alert('哈哈')</script>"，
 * 如果这个字符串原封不动显示在网页上，浏览器会把它当成真正的脚本执行。
 *
 * 这就是 XSS（跨站脚本攻击），是 Web 开发中最常见的漏洞之一。
 *
 * 转义的作用：
 *   < 变成 &lt;   → 浏览器显示"<"但不把它当标签
 *   > 变成 &gt;   → 浏览器显示">"但不把它当标签
 *   " 变成 &quot; → 防止提前闭合 HTML 属性
 *   ' 变成 &#39;  → 防止提前闭合 HTML 属性
 *   & 变成 &amp;  → 防止和转义字符本身冲突（必须最先转）
 *
 * 为什么 & 必须最先转？
 * 如果先转了 < 成 &lt;，&lt; 中的 & 又会被转成 &amp;lt;，
 * 最终显示就乱套了。所以 & 永远排第一个。
 *
 * 生活中的类比：
 * 就像出入境检验检疫 —— 把有害的东西（脚本标签）拦在门外，
 * 但让无害的东西（正常文字）顺利通过。
 */
public class HtmlUtil {

    /**
     * 私有构造方法 —— 纯工具类不需要被 new。
     */
    private HtmlUtil() {
    }

    /**
     * 对字符串做 HTML 转义，使其可以安全地嵌入 HTML 页面。
     *
     * 调用时机：
     * JSP 页面中用 ${HtmlUtil.escape(变量)} 输出用户数据时调用。
     * 所有来自用户输入的内容、来自数据库的内容，展示前都必须转义。
     *
     * 为什么接收 Object 而不是 String？
     * JSP 中拿到的属性可能是各种类型（数字、日期等），
     * 用 Object 做参数可以统一处理，不需要调用方先转成 String。
     * 内部用 String.valueOf() 转成字符串再处理。
     *
     * 什么时候不需要转义？
     * 只有当数据插入 HTML 标签的内容区域时才需要。
     * 如果数据只是用于 URL 参数、CSS、JavaScript 等场景，
     * 需要不同的转义规则。
     *
     * @param value 要转义的值（可以是任何类型，null 会变成空字符串）
     * @return 转义后的安全字符串
     */
    public static String escape(Object value) {
        // null 值在网页上显示为空，而不是"null"四个字
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        // 注意：& 必须第一个替换！
        // 因为后续的 &lt; &gt; &quot; &#39; 都包含 & 字符，
        // 如果后替换 &，会把它们的 & 也转义掉，导致双重转义。
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
