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
 * 问卷列表控制器。
 */
@WebServlet("/vote/list")
public class VoteListServlet extends HttpServlet {
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
            request.setAttribute("questions", VoteModel.findAllQuestions());
            request.setAttribute("currentUser", user);
            request.getRequestDispatcher("/vote_list.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询问卷列表失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_list.jsp").forward(request, response);
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }
}
