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
 * 投票提交控制器 —— 用户选择选项并提交投票。
 *
 * 这是整个系统中防重复投票的关键位置。
 *
 *
 * ============ 防重复投票机制 ============
 *
 * 需求：每个用户对一个问卷只能投一次票。
 *
 * 实现方案：Session 投票记录
 *   在 Session 中维护一个 HashSet<Integer>，
 *   存储用户已经投过的问卷编号。
 *
 *   投票前：检查这个问卷 ID 是否在集合中 → 在就拦截
 *   投票后：把问卷 ID 加入集合
 *
 * 为什么用 Session 而不是数据库？
 *   1. Session 读取速度快（内存 vs 磁盘）
 *   2. Session 不需要额外的数据库表
 *   3. 课程设计规模小，Session 方案足够
 *
 * 但这种方案有局限：
 *   更换浏览器或清除 Cookie 后会丢失记录，可以重新投票。
 *   生产环境中应该在数据库里建一张 vote_records 表，
 *   记录（用户ID, 问卷ID）的唯一对应关系。
 *
 *
 * ============ GET vs POST 的设计 ============
 *
 * doGet：用户点击"去投票"链接 → 展示投票页面（问卷标题 + 选项列表）
 * doPost：用户选中一个选项并提交 → 记录投票
 *
 * 这样设计符合 HTTP 规范：GET 用于展示，POST 用于提交。
 */
@WebServlet("/vote/submit")
public class VoteSubmitServlet extends HttpServlet {

    /**
     * GET —— 展示投票页面。
     *
     * 需要查询的数据：
     *   - 问卷标题（来自 vote_questions 表）
     *   - 选项列表（来自 vote_options 表）
     *   - 是否已投过票（来自 Session）
     *
     * 如果已经投过票，页面会显示"你已投过票"的提示，
     * 而不是投票表单。
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
            // questionId 不合法：可能是手动改了 URL，或者链接错误
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

    /**
     * POST —— 处理投票提交。
     *
     * 安全校验顺序（每一步失败都会拦截）：
     *   1. 是否登录？
     *   2. 问卷 ID 和选项 ID 是否有效？
     *   3. 是否已经投过这个问卷了？
     *   4. 问卷是否真实存在？
     *   5. 选项是否真的属于这个问卷？（防止篡改 optionId）
     *
     * 全部通过后才真正增加票数，并记录到已投票集合中。
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
        int optionId = parseInt(request.getParameter("optionId"));

        // 问卷编号或选项编号无效
        if (questionId <= 0 || optionId <= 0) {
            request.setAttribute("error", "请选择一个有效选项");
            forwardSubmitPage(request, response, questionId);
            return;
        }

        // 防重复投票检查
        Set<Integer> votedQuestionIds = getVotedQuestionIds(request.getSession());
        if (votedQuestionIds.contains(Integer.valueOf(questionId))) {
            // 已经投过了 → 直接跳去结果页，不给重复投票的机会
            response.sendRedirect(request.getContextPath() + "/vote/result?questionId=" + questionId);
            return;
        }

        try {
            // 确认问卷存在
            VoteQuestion question = VoteModel.findQuestionById(questionId);
            if (question == null) {
                request.setAttribute("error", "问卷不存在");
                request.getRequestDispatcher("/vote_submit.jsp").forward(request, response);
                return;
            }

            // 确认选项属于当前问卷（防止篡改参数把票投到别的问卷上）
            if (!VoteModel.optionBelongsToQuestion(optionId, questionId)) {
                request.setAttribute("error", "选项不属于当前问卷");
                forwardSubmitPage(request, response, questionId);
                return;
            }

            // 所有检查通过，执行投票
            VoteModel.increaseOptionVoteCount(optionId);

            // 标记"已投票"，防止下次再投
            votedQuestionIds.add(Integer.valueOf(questionId));
            request.getSession().setAttribute("votedQuestionIds", votedQuestionIds);

            // 投完票直接跳去结果页看答案
            response.sendRedirect(request.getContextPath() + "/vote/result?questionId=" + questionId);
        } catch (SQLException e) {
            request.setAttribute("error", "提交投票失败：" + e.getMessage());
            forwardSubmitPage(request, response, questionId);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 为投票页面加载问卷和选项数据。
     *
     * 同时检查用户是否已经投过票，
     * 把结果传给 JSP 用于决定显示"投票表单"还是"已投票提示"。
     */
    private void loadQuestionForSubmit(HttpServletRequest request, int questionId) throws SQLException {
        VoteQuestion question = VoteModel.findQuestionById(questionId);
        if (question == null) {
            request.setAttribute("error", "问卷不存在");
            return;
        }
        request.setAttribute("question", question);
        request.setAttribute("options", VoteModel.findOptionsByQuestionId(questionId));
        // 告诉 JSP 页面：这个用户是否已经投过了
        request.setAttribute("alreadyVoted",
                Boolean.valueOf(getVotedQuestionIds(request.getSession()).contains(Integer.valueOf(questionId))));
    }

    /**
     * 出错时重新加载数据并转发回投票页面。
     *
     * 为什么需要这个方法？
     * 提交投票失败时（如选项无效），不能直接 forward 回 JSP，
     * 因为 JSP 需要问卷和选项数据才能渲染。
     * 这个方法负责重新加载这些数据，然后转发。
     */
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

    /**
     * 从 Session 获取"已投票问卷集合"。
     *
     * 为什么用 HashSet 而不是 ArrayList？
     * HashSet 的 contains() 查找速度是 O(1)，
     * ArrayList 是 O(n)。用户投的票越多，差距越明显。
     *
     * 泛型警告 @SuppressWarnings("unchecked")：
     * Session 存的是 Object，取出来强转成 Set<Integer> 时
     * 编译器会警告。但我们能确保存进去的一定是 Set<Integer>，
     * 所以可以安全地忽略这个警告。
     */
    @SuppressWarnings("unchecked")
    private Set<Integer> getVotedQuestionIds(HttpSession session) {
        Object value = session.getAttribute("votedQuestionIds");
        if (value instanceof Set) {
            return (Set<Integer>) value;
        }
        // 第一次投票，Session 中还没有这个集合 → 创建一个新的
        Set<Integer> votedQuestionIds = new HashSet<Integer>();
        session.setAttribute("votedQuestionIds", votedQuestionIds);
        return votedQuestionIds;
    }

    /**
     * 从 Session 中取出当前登录的用户。
     */
    private User getCurrentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    /**
     * 安全字符串转整数。
     *
     * 为什么不直接用 Integer.parseInt()？
     * 因为 URL 参数可能被用户手动修改为非数字（如 ?questionId=abc）。
     * parseInt 会把解析失败的情况统一返回 -1，
     * 调用方通过判断 <= 0 就能拦截非法输入。
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return -1; // -1 在数据库主键中不存在，调用方会当作非法值处理
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
