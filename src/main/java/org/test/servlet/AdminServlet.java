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
 * 管理员控制器 —— 处理问卷审批和结束操作。
 *
 * 这个 Servlet 是管理员专属的操作入口。
 * 所有操作在执行前都会校验管理员身份，
 * 确保普通用户无法通过直接访问 URL 来执行管理员操作。
 *
 * ============ 安全设计 ============
 *
 * 每个操作内部都做了双重校验：
 *   1. 登录校验 —— 必须登录才能操作
 *   2. 管理员或权限校验 —— 审批只有管理员能做，
 *      结束可以由管理员或发布者做
 *
 * 校验在服务器端进行，不依赖前端隐藏按钮。
 * 即使有人猜到 URL 并发请求，没有权限也无法操作成功。
 *
 * action 参数说明：
 *   approve → 审批通过一个待审批的问卷
 *   end     → 结束一个已通过的问卷
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    /**
     * GET 请求 —— 暂不处理。
     * 管理功能通过问卷列表页面的按钮触发 POST 请求。
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 管理操作通过列表页上的按钮发起 POST，不通过 GET
        response.sendRedirect(request.getContextPath() + "/vote/list");
    }

    /**
     * POST 请求 —— 根据 action 参数分发到具体操作。
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

        String action = request.getParameter("action");

        if ("approve".equals(action)) {
            approve(request, response, user);
        } else if ("end".equals(action)) {
            end(request, response, user);
        } else {
            // 未知操作，跳回列表
            response.sendRedirect(request.getContextPath() + "/vote/list");
        }
    }

    // ==================== 审批操作 ====================

    /**
     * 审批通过一个问卷。
     *
     * 校验链：
     *   1. questionId 必须有效
     *   2. 当前用户必须是管理员（非管理员无权审批）
     *   3. 问卷状态必须为 pending（只能审批待审批的问卷）
     *
     * 审批成功后重定向到列表页，管理员能看到该问卷移到已通过区域。
     */
    private void approve(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        int questionId = parseInt(request.getParameter("questionId"));
        if (questionId <= 0) {
            request.setAttribute("error", "问卷编号不合法");
            forwardToList(request, response);
            return;
        }

        // 权限校验：只有管理员可以审批
        if (!"admin".equals(user.getRole())) {
            request.setAttribute("error", "只有管理员可以审批问卷");
            forwardToList(request, response);
            return;
        }

        try {
            if (!VoteModel.approveQuestion(questionId)) {
                // approveQuestion 失败：问卷不存在或状态不是 pending
                request.setAttribute("error", "审批失败：问卷不存在或状态不正确（只能审批待审批的问卷）");
            }
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "审批失败：" + e.getMessage());
            forwardToList(request, response);
        }
    }

    // ==================== 结束操作 ====================

    /**
     * 结束一个正在进行的问卷。
     *
     * 校验链：
     *   1. questionId 必须有效
     *   2. 当前用户必须是管理员或问卷发布者
     *   3. 问卷状态必须为 approved（只能结束已通过的问卷）
     *
     * 结束后该问卷不会再显示"去投票"按钮。
     */
    private void end(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        int questionId = parseInt(request.getParameter("questionId"));
        if (questionId <= 0) {
            request.setAttribute("error", "问卷编号不合法");
            forwardToList(request, response);
            return;
        }

        try {
            // 权限校验：管理员或发布者可以结束问卷
            if (!VoteModel.isQuestionOwnerOrAdmin(questionId, user.getId())) {
                request.setAttribute("error", "只能结束自己发布的问卷（管理员可以结束任意问卷）");
                forwardToList(request, response);
                return;
            }

            if (!VoteModel.endQuestion(questionId)) {
                request.setAttribute("error", "结束失败：问卷不存在或状态不正确（只能结束已通过的问卷）");
            }
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "操作失败：" + e.getMessage());
            forwardToList(request, response);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 出错时重新加载列表并转发。
     */
    private void forwardToList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User user = getCurrentUser(request);
            if ("admin".equals(user != null ? user.getRole() : null)) {
                request.setAttribute("questions", VoteModel.findAllQuestionsForAdmin());
            } else {
                request.setAttribute("questions", VoteModel.findQuestionsForUser());
            }
            request.setAttribute("currentUser", user);
        } catch (SQLException e) {
            request.setAttribute("error", "加载问卷列表失败：" + e.getMessage());
        }
        request.getRequestDispatcher("/vote_list.jsp").forward(request, response);
    }

    private User getCurrentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return -1;
        }
    }

    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }
}
