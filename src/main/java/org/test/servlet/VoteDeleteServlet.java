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
 * 问卷删除控制器。
 *
 * 删除前必须确认当前登录用户就是问卷发布者。
 */
@WebServlet("/vote/delete")
public class VoteDeleteServlet extends HttpServlet {
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
                request.setAttribute("error", "只能删除自己发布的问卷");
            } else {
                request.setAttribute("question", question);
            }
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询问卷失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
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
        if (questionId <= 0) {
            request.setAttribute("error", "问卷编号不合法");
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
            return;
        }

        try {
            if (!VoteModel.isQuestionOwner(questionId, user.getId())) {
                request.setAttribute("error", "只能删除自己发布的问卷");
                request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
                return;
            }

            VoteModel.deleteQuestion(questionId);
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "删除问卷失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_delete.jsp").forward(request, response);
        }
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
