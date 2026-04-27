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
 * 问卷发布控制器。
 */
@WebServlet("/vote/publish")
public class VotePublishServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);
        User user = getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/user?action=login");
            return;
        }
        request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
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

        String title = trim(request.getParameter("title"));
        List<String> options = collectOptions(request.getParameterValues("options"));

        if (title.length() == 0) {
            request.setAttribute("error", "问卷标题不能为空");
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
            return;
        }
        if (options.size() < 2) {
            request.setAttribute("error", "至少需要填写两个有效选项");
            request.setAttribute("title", title);
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
            return;
        }

        try {
            VoteModel.createQuestion(user.getId(), title, options);
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "发布失败：" + e.getMessage());
            request.setAttribute("title", title);
            request.getRequestDispatcher("/vote_publish.jsp").forward(request, response);
        }
    }

    private List<String> collectOptions(String[] values) {
        List<String> options = new ArrayList<String>();
        if (values == null) {
            return options;
        }
        for (String value : values) {
            String option = trim(value);
            if (option.length() > 0) {
                options.add(option);
            }
        }
        return options;
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
