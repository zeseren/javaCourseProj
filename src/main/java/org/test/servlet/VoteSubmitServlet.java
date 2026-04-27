package org.test.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.test.model.User;
import org.test.model.VoteModel;
import org.test.model.VoteQuestion;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 投票提交控制器。
 *
 * 这里使用 Session 保存已经投过的问卷 ID，满足课程要求中的防重复投票。
 */
@WebServlet("/vote/submit")
public class VoteSubmitServlet extends HttpServlet {
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
            request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
            return;
        }

        try {
            loadQuestionForSubmit(request, questionId);
            request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询问卷失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
        }
    }

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
        int optionId = parseInt(request.getParameter("optionId"));

        if (questionId <= 0 || optionId <= 0) {
            request.setAttribute("error", "请选择一个有效选项");
            forwardSubmitPage(request, response, questionId);
            return;
        }

        Set<Integer> votedQuestionIds = getVotedQuestionIds(request.getSession());
        if (votedQuestionIds.contains(Integer.valueOf(questionId))) {
            response.sendRedirect(request.getContextPath() + "/vote/result?questionId=" + questionId);
            return;
        }

        try {
            VoteQuestion question = VoteModel.findQuestionById(questionId);
            if (question == null) {
                request.setAttribute("error", "问卷不存在");
                request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
                return;
            }
            if (!VoteModel.optionBelongsToQuestion(optionId, questionId)) {
                request.setAttribute("error", "选项不属于当前问卷");
                forwardSubmitPage(request, response, questionId);
                return;
            }

            VoteModel.increaseOptionVoteCount(optionId);
            votedQuestionIds.add(Integer.valueOf(questionId));
            request.getSession().setAttribute("votedQuestionIds", votedQuestionIds);
            response.sendRedirect(request.getContextPath() + "/vote/result?questionId=" + questionId);
        } catch (SQLException e) {
            request.setAttribute("error", "提交投票失败：" + e.getMessage());
            forwardSubmitPage(request, response, questionId);
        }
    }

    private void loadQuestionForSubmit(HttpServletRequest request, int questionId) throws SQLException {
        VoteQuestion question = VoteModel.findQuestionById(questionId);
        if (question == null) {
            request.setAttribute("error", "问卷不存在");
            return;
        }
        request.setAttribute("question", question);
        request.setAttribute("options", VoteModel.findOptionsByQuestionId(questionId));
        request.setAttribute("alreadyVoted",
                Boolean.valueOf(getVotedQuestionIds(request.getSession()).contains(Integer.valueOf(questionId))));
    }

    private void forwardSubmitPage(HttpServletRequest request, HttpServletResponse response, int questionId)
            throws ServletException, IOException {
        try {
            if (questionId > 0) {
                loadQuestionForSubmit(request, questionId);
            }
        } catch (SQLException e) {
            request.setAttribute("error", "查询问卷失败：" + e.getMessage());
        }
        request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> getVotedQuestionIds(HttpSession session) {
        Object value = session.getAttribute("votedQuestionIds");
        if (value instanceof Set) {
            return (Set<Integer>) value;
        }
        Set<Integer> votedQuestionIds = new HashSet<Integer>();
        session.setAttribute("votedQuestionIds", votedQuestionIds);
        return votedQuestionIds;
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
