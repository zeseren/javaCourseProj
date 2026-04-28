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

/**
 * 问卷列表控制器 —— 展示所有投票问卷的首页。
 *
 * 这是用户登录后看到的第一个页面。
 * 页面展示内容：
 *   - 所有问卷的列表（标题、发布者、发布时间）
 *   - 每个问卷后面的操作按钮（投票、删除等）
 *
 * 为什么这里要重新把 currentUser 放进 request？
 * 因为 JSP 页面（vote_list.jsp）需要知道当前用户是谁，
 * 比如判断某个问卷是不是"我"发布的，从而决定是否显示"删除"按钮。
 * 虽然 Session 里有 currentUser，但直接在 JSP 中访问 Session
 * 会破坏 MVC 的分层 —— JSP 应该只从 request 中取数据。
 */
@WebServlet("/vote/list")
public class VoteListServlet extends HttpServlet {

    /**
     * GET —— 展示问卷列表。
     *
     * 这个 Servlet 只响应 GET 请求，因为列表页只需要展示数据，
     * 不涉及表单提交。
     *
     * 流程：
     *   1. 检查登录状态 → 未登录就跳去登录
     *   2. 从数据库查询所有问卷
     *   3. 把问卷列表和当前用户信息传给 JSP 展示
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        User user = getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/user?action=login");
            return;
        }

        try {
            // 查询所有问卷，按时间倒序
            request.setAttribute("questions", VoteModel.findAllQuestions());
            // 把当前用户也传给 JSP，用于判断"是不是我的问卷"
            request.setAttribute("currentUser", user);
            request.getRequestDispatcher("/vote_list.jsp").forward(request, response);
        } catch (SQLException e) {
            // 查询失败时仍然展示列表页，但显示错误信息
            request.setAttribute("error", "查询问卷列表失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_list.jsp").forward(request, response);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 Session 中取出当前登录的用户。
     */
    private User getCurrentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    /**
     * 统一设置编码，防止中文乱码。
     */
    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }
}
