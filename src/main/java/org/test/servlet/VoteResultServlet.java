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
 * 投票结果控制器 —— 展示一个问卷的投票统计。
 *
 * 这个 Servlet 的职责：
 *   1. 查询问卷基本信息（标题、发布者）
 *   2. 查询所有选项及其得票数
 *   3. 计算每个选项的得票比例
 *   4. 计算总投票数
 *   5. 把以上数据传给 JSP 渲染
 *
 *
 * ============ 比例计算的设计 ============
 *
 * 为什么不在数据库中存比例？
 * 因为每次有人投票，所有选项的比例都会变化。
 * 如果存数据库，每投一票就要更新所有行的 percent，
 * 不仅写操作多，还有并发问题。
 *
 * 每次展示时实时计算：
 *   percent = (当前选项票数 / 总票数) × 100
 *
 * 这样永远准确，也不需要额外存储。
 *
 *
 * ============ 除零保护 ============
 *
 * 总票数可能为 0（还没有任何人投票）。
 * 此时如果直接除会抛异常。
 * 代码中做了保护：totalVotes == 0 时，比例直接设为 0。
 */
@WebServlet("/vote/result")
public class VoteResultServlet extends HttpServlet {

    /**
     * GET —— 展示投票结果页面。
     *
     * 通过 URL 参数 questionId 决定看哪个问卷的结果。
     * 例如：/vote/result?questionId=3 → 查看 3 号问卷的结果。
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
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
            return;
        }

        try {
            // 第一步：查询问卷本身
            VoteQuestion question = VoteModel.findQuestionById(questionId);
            if (question == null) {
                request.setAttribute("error", "问卷不存在");
                request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
                return;
            }

            // 第二步：查询所有选项及其票数
            List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);

            // 第三步：计算总票数（用于算比例）
            int totalVotes = 0;
            for (VoteOption option : options) {
                totalVotes += option.getVoteCount();
            }

            // 第四步：计算每个选项的百分比
            for (VoteOption option : options) {
                double percent = totalVotes == 0
                        ? 0  // 还没有任何人投票 → 比例就是 0
                        : option.getVoteCount() * 100.0 / totalVotes;  // 正常计算
                option.setPercent(percent);
            }

            // 第五步：把数据打包传给 JSP
            request.setAttribute("question", question);
            request.setAttribute("options", options);
            request.setAttribute("totalVotes", Integer.valueOf(totalVotes)); // 用包装类型方便 JSP 使用
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "查询投票结果失败：" + e.getMessage());
            request.getRequestDispatcher("/vote_result.jsp").forward(request, response);
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
     * 安全字符串转整数（非法输入返回 -1）。
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
