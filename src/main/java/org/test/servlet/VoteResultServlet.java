package org.test.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.test.model.User;
import org.test.model.VoteModel;
import org.test.model.VoteOption;
import org.test.model.VoteQuestion;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 投票结果控制器。
 */
@WebServlet("/vote/result")
public class VoteResultServlet extends HttpServlet {
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
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
            return;
        }

        try {
            VoteQuestion question = VoteModel.findQuestionById(questionId);
            if (question == null) {
                request.setAttribute("error", "问卷不存在");
                request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
                return;
            }

            List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);
            int totalVotes = 0;
            for (VoteOption option : options) {
                totalVotes += option.getVoteCount();
            }
            for (VoteOption option : options) {
                double percent = totalVotes == 0 ? 0 : option.getVoteCount() * 100.0 / totalVotes;
                option.setPercent(percent);
            }

            request.setAttribute("question", question);
            request.setAttribute("options", options);
            request.setAttribute("totalVotes", Integer.valueOf(totalVotes));
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询投票结果失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
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
