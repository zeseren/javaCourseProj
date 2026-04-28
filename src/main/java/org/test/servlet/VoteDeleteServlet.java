package org.test.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.test.model.User;
import org.test.model.VoteModel;
import org.test.model.VoteQuestion;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 问卷删除控制器 —— 允许用户删除自己发布的问卷。
 *
 *
 * ============ 权限控制 ============
 *
 * 删除操作有严格的权限要求：
 *   只有问卷的发布者本人才可以删除。
 *
 * 为什么在 doGet 和 doPost 中都要检查权限？
 *   doGet：展示删除确认页面前检查 —— 防止用户通过 URL 看到别人的删除页面
 *   doPost：真正执行删除前再检查一次 —— 防止绕过页面直接发 POST 请求
 *
 * 这是"纵深防御"的做法：每一层都做检查，而不是假设上一层已经检查过了。
 *
 *
 * ============ GET-POST 配合 ============
 *
 * doGet：展示确认页面（"确定要删除 XXX 问卷吗？"）
 * doPost：执行真正的删除操作
 *
 * 为什么删除需要两步（确认 → 执行）？
 * 防止用户误点"删除"按钮导致数据丢失。
 * 多一步确认，给用户一个"反悔"的机会。
 *
 * 如果删除成功，重定向到问卷列表页。
 * 如果删除失败（如权限不足），留在当前页面并显示错误。
 */
@WebServlet("/vote/delete")
public class VoteDeleteServlet extends HttpServlet {

    /**
     * GET —— 展示删除确认页面。
     *
     * 执行逻辑：
     *   1. 检查登录
     *   2. 检查 questionId 合法性
     *   3. 查询问卷是否存在
     *   4. 检查当前用户是否是发布者
     *   5. 通过则显示问卷信息，让用户确认删除
     *
     * 注意：这一步并不会执行删除，只是展示确认页面。
     * 真正的删除在 doPost 中执行。
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

        int questionId = parseInt(request.getParameter("questionId"));
        if (questionId <= 0) {
            request.setAttribute("error", "问卷编号不合法");
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
            return;
        }

        try {
            VoteQuestion question = VoteModel.findQuestionById(questionId);
            if (question == null) {
                request.setAttribute("error", "问卷不存在");
            } else if (!VoteModel.isQuestionOwner(questionId, user.getId())) {
                // 不是发布者 → 不能删除别人的问卷
                request.setAttribute("error", "只能删除自己发布的问卷");
            } else {
                // 一切正常 → 让用户确认
                request.setAttribute("question", question);
            }
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询问卷失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
        }
    }

    /**
     * POST —— 执行真正的删除操作。
     *
     * 即使 GET 阶段已经检查过权限，
     * POST 阶段仍然再次检查，防止以下攻击方式：
     *   用户 A 登录后，用工具构造一个直接发送到 POST 的请求，
     *   试图删除用户 B 的问卷。
     *
     * 删除成功后：
     *   重定向到问卷列表页，用户能看到自己发布的其他问卷。
     *
     * 删除失败时：
     *   留在删除确认页，显示具体错误原因。
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

        int questionId = parseInt(request.getParameter("questionId"));
        if (questionId <= 0) {
            request.setAttribute("error", "问卷编号不合法");
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
            return;
        }

        try {
            // 再次校验权限 —— 纵深防御
            if (!VoteModel.isQuestionOwner(questionId, user.getId())) {
                request.setAttribute("error", "只能删除自己发布的问卷");
                request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
                return;
            }

            // 权限通过，执行删除
            VoteModel.deleteQuestion(questionId);
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "删除问卷失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
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
     * 安全字符串转整数。
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 统一设置编码。
     */
    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }
}
