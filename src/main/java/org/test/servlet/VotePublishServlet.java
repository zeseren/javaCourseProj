package org.test.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.test.model.User;
import org.test.model.VoteModel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 问卷发布控制器 —— 创建新的投票问卷。
 *
 * 访问流程：
 *   1. 用户点击"发布问卷"链接 → GET 请求 → 展示发布页面
 *   2. 用户填写标题和选项 → POST 请求 → 写入数据库
 *
 * 安全控制：
 *   必须登录才能访问。未登录用户会被重定向到登录页。
 *   这个检查在 doGet 和 doPost 中都有，防止通过绕过页面直接发请求。
 *
 *
 * 选项的设计思路：
 * ────────────────
 * 前端表单中，多个选项使用相同的 name="options"，
 * 后端通过 getParameterValues("options") 拿到一个数组。
 *
 * HTML 表单示例：
 *   <input name="options" value="Java">
 *   <input name="options" value="Python">
 *   <input name="options" value="Go">
 *
 * 这样的设计允许用户自由增减选项数量，
 * 不需要限制只能是固定数量的选项。
 */
@WebServlet("/vote/publish")
public class VotePublishServlet extends HttpServlet {

    /**
     * GET —— 展示问卷发布页面。
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        User user = getCurrentUser(request);
        if (user == null) {
            // 未登录 → 请先去登录
            response.sendRedirect(request.getContextPath() + "/user?action=login");
            return;
        }

        // 已登录 → 展示发布页面
        request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
    }

    /**
     * POST —— 处理问卷发布表单。
     *
     * 发布前的服务器端校验（浏览器端的 JS 校验可能被绕过）：
     *   1. 必须登录
     *   2. 标题不能为空
     *   3. 至少要有 2 个有效选项
     *
     * 校验通过后调用 VoteModel.createQuestion() 写入数据库，
     * 成功则重定向到问卷列表。
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        User user = getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/user?action=login");
            return;
        }

        String title = trim(request.getParameter("title"));
        List<String> options = collectOptions(request.getParameterValues("options"));

        // 标题校验
        if (title.length() == 0) {
            request.setAttribute("error", "问卷标题不能为空");
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
            return;
        }

        // 选项数量校验 —— 至少两个选项才有投票意义
        if (options.size() < 2) {
            request.setAttribute("error", "至少需要填写两个有效选项");
            request.setAttribute("title", title); // 保留已填的标题，减少用户重复输入
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
            return;
        }

        try {
            VoteModel.createQuestion(user.getId(), title, options);
            // 发布成功后重定向到列表页，用户能看到新问卷出现在列表中
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "发布失败：" + e.getMessage());
            request.setAttribute("title", title);
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从请求参数中收集有效选项。
     *
     * 为什么要把收集逻辑单独写？
     * 因为 getParameterValues 返回的数组可能：
     *   - 为 null（一个选项都没填）
     *   - 包含空字符串（用户输入了空白行）
     *   - 包含只有空格的字符串（用户打了几个空格）
     *
     * 这个方法把这些脏数据全部过滤掉，
     * 只留下真正有内容的选项。
     *
     * @param values 表单中所有 name="options" 的输入值
     * @return 过滤后的有效选项列表（不会为 null，但可能是空列表）
     */
    private List<String> collectOptions(String[] values) {
        List<String> options = new ArrayList<String>();
        if (values == null) {
            return options; // 空列表，不是 null
        }
        for (String value : values) {
            String option = trim(value);
            if (option.length() > 0) {
                options.add(option);
            }
        }
        return options;
    }

    /**
     * 从 Session 中取出当前登录的用户。
     *
     * 为什么用 instanceof 检查？
     * Session 中存的是 Object 类型，理论上可以存任何东西。
     * 虽然我们只会存 User，但做一次类型检查更安全。
     * 如果类型不对，返回 null 让调用方走"未登录"流程。
     */
    private User getCurrentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    /**
     * 统一设置请求响应编码，防止中文乱码。
     */
    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }

    /**
     * 安全去除首尾空格（null 转为空字符串）。
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
