package org.test.model;

import org.test.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 投票数据访问层 —— 所有和 vote_questions、vote_options 表相关的操作。
 *
 * 设计要点：事务处理
 * ─────────────────
 * 发布问卷时，需要同时做两件事：
 *   1. 向 vote_questions 表插入问卷
 *   2. 向 vote_options 表插入多个选项
 *
 * 这两步必须"要么全做，要么全不做"。如果第 2 步失败了，
 * 数据库里就会留一个没有选项的"空壳问卷"，用户看到会报错。
 *
 * 生活中的类比：
 * 转账 = 从 A 账户扣钱 + 给 B 账户加钱。
 * 如果只扣钱没加钱，钱就凭空消失了。
 * 事务就是保证两步都完成，否则全部撤销。
 *
 * 代码里通过 conn.setAutoCommit(false) + conn.commit() + conn.rollback()
 * 来实现事务控制。
 */
public class VoteModel {

    // 不让外部 new，因为这个类只有 static 工具方法
    private VoteModel() {
    }

    /**
     * 发布一个新问卷 —— 包含事务控制的复合操作。
     *
     * 执行流程（每一步都不能出错）：
     *   1. 开启事务（关闭自动提交）
     *   2. 插入问卷标题到 vote_questions 表
     *   3. 拿到数据库自动生成的问卷编号
     *   4. 用这个编号批量插入所有选项到 vote_options 表
     *   5. 全部成功后提交事务
     *
     * 如果中间任何一步失败，整个事务回滚，数据库回到操作前的状态。
     *
     * 为什么不需要传 username？
     * 因为 vote_questions 表只存 user_id（数字编号），
     * 显示名字时临时从 users 表 JOIN 查询即可。
     * 这样如果用户改名了，所有历史问卷显示的名都会自动更新。
     *
     * @param userId  发布者的用户编号
     * @param title   问卷标题
     * @param options 选项列表（至少 2 个，Servlet 层已校验）
     * @return 发布成功返回 true，获取不到自增 ID 则回滚并返回 false
     * @throws SQLException 数据库错误
     */
    public static boolean createQuestion(int userId, String title, List<String> options) throws SQLException {
        // 新发布的问卷状态为 pending（待审批），需要管理员审批后才能被其他用户看到
        String questionSql = "insert into vote_questions(title, user_id, status) values(?, ?, 'pending')";
        String optionSql = "insert into vote_options(question_id, content) values(?, ?)";

        Connection conn = null;
        PreparedStatement questionPs = null;
        PreparedStatement optionPs = null;
        ResultSet keys = null;
        try {
            conn = JdbcUtil.getConnection();
            // 关闭自动提交，后续手动控制 commit 和 rollback
            conn.setAutoCommit(false);

            // 第 1 步：插入问卷，同时要求数据库返回自动生成的 ID
            questionPs = conn.prepareStatement(questionSql, Statement.RETURN_GENERATED_KEYS);
            questionPs.setString(1, title);
            questionPs.setInt(2, userId);
            questionPs.executeUpdate();

            // 第 2 步：拿回数据库自动生成的问卷 ID
            keys = questionPs.getGeneratedKeys();
            if (!keys.next()) {
                // 拿不到 ID 说明插入可能有问题，回滚所有操作
                conn.rollback();
                return false;
            }
            int questionId = keys.getInt(1);

            // 第 3 步：批量插入选项（使用 batch 减少数据库通信次数）
            optionPs = conn.prepareStatement(optionSql);
            for (String option : options) {
                optionPs.setInt(1, questionId);
                optionPs.setString(2, option);
                optionPs.addBatch(); // 添加到批次，暂不执行
            }
            optionPs.executeBatch(); // 一次性提交所有选项

            // 第 4 步：全部成功，提交事务
            conn.commit();
            return true;
        } catch (SQLException e) {
            // 任何一步出错都要回滚，防止出现"有问卷没选项"的情况
            if (conn != null) {
                conn.rollback();
            }
            throw e; // 重新抛出，让 Servlet 层决定怎么给用户提示
        } finally {
            // 逐一关闭资源，避免内存泄漏
            // keys 和 questionPs 需要先关闭，因为它们依赖同一个连接
            if (keys != null) {
                try {
                    keys.close();
                } catch (SQLException ignored) {
                    // 关闭时出错不影响主流程，忽略即可
                }
            }
            if (questionPs != null) {
                try {
                    questionPs.close();
                } catch (SQLException ignored) {
                }
            }
            JdbcUtil.close(optionPs, conn);
        }
    }

    /**
     * 查询所有问卷 —— 用于首页列表展示。
     *
     * 为什么用 JOIN 而不是分两次查询？
     * 如果先查问卷表，再循环查每个问卷的用户名，
     * 会产生"N+1 次查询"（1 次问卷 + N 个问卷各查 1 次用户名）。
     * JOIN 一次就把所有数据拿出来，效率远超多次查询。
     *
     * 为什么按 created_at 降序？
     * 用户最关心最新发布的问卷，越新的越应该排在最上面。
     *
     * @return 所有问卷的列表（可能是空列表，不会是 null）
     * @throws SQLException 数据库错误
     */
    public static List<VoteQuestion> findAllQuestions() throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.status, q.created_at "
                + "from vote_questions q join users u on q.user_id = u.id "
                + "order by q.created_at desc";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            List<VoteQuestion> questions = new ArrayList<VoteQuestion>();
            while (rs.next()) {
                questions.add(mapQuestion(rs));
            }
            return questions;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 根据编号查询单个问卷。
     *
     * 使用场景：
     * - 投票页展示问卷标题
     * - 结果页展示问卷标题
     * - 删除前确认问卷是否存在
     *
     * @param questionId 问卷编号
     * @return 找到返回问卷对象，找不到返回 null
     * @throws SQLException 数据库错误
     */
    public static VoteQuestion findQuestionById(int questionId) throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.status, q.created_at "
                + "from vote_questions q join users u on q.user_id = u.id "
                + "where q.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapQuestion(rs);
            }
            return null;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 查询某个问卷下的所有选项。
     *
     * 为什么按 id 排序？
     * 保证选项展示顺序和创建顺序一致，
     * 否则每次刷新页面，选项顺序可能会变。
     *
     * @param questionId 问卷编号
     * @return 选项列表（无选项时返回空列表）
     * @throws SQLException 数据库错误
     */
    public static List<VoteOption> findOptionsByQuestionId(int questionId) throws SQLException {
        String sql = "select id, question_id, content, vote_count from vote_options where question_id = ? order by id";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            rs = ps.executeQuery();
            List<VoteOption> options = new ArrayList<VoteOption>();
            while (rs.next()) {
                options.add(mapOption(rs));
            }
            return options;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 给指定选项的票数 +1。
     *
     * 为什么用 UPDATE ... SET vote_count = vote_count + 1 而不是先查再改？
     * 因为"先查后改"存在竞态条件（race condition）：
     *   1. 用户 A 查到票数为 5
     *   2. 用户 B 也查到票数为 5
     *   3. A 把票数改成 6
     *   4. B 也把票数改成 6 ← 错误！应该是 7
     *
     * 直接在数据库里 +1 是原子操作，数据库会保证并发安全。
     *
     * 本课程设计中通过 Session 防止同一用户重复投票，
     * 所以不需要在 SQL 层面做额外的防刷限制。
     *
     * @param optionId 选项编号
     * @return 成功更新了一行则返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean increaseOptionVoteCount(int optionId) throws SQLException {
        String sql = "update vote_options set vote_count = vote_count + 1 where id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, optionId);
            return ps.executeUpdate() == 1;
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    /**
     * 检查某个用户是否是问卷的发布者。
     *
     * 使用场景：删除问卷前做权限校验。
     * 只有发布者本人才能删除自己的问卷，防止恶意删除。
     *
     * 为什么用 count(*) 而不是直接查一行？
     * count(*) 只返回一个数字，不需要取具体数据，
     * 数据库执行效率更高（可能只走索引就够了）。
     *
     * @param questionId 问卷编号
     * @param userId     用户编号
     * @return 是该用户发布的返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean isQuestionOwner(int questionId, int userId) throws SQLException {
        String sql = "select count(*) from vote_questions where id = ? and user_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 验证一个选项确实属于指定的问卷。
     *
     * 为什么需要这个校验？
     * 投票提交的参数是通过 URL 传来的（optionId），
     * 恶意用户可能会篡改这个值，把选项指向另一个问卷。
     * 如果不校验，就可能出现"投问卷 A 的票却记在了问卷 B 上"。
     *
     * 这是一种防御性编程的做法 —— 永远不要信任客户端传来的数据。
     *
     * @param optionId   选项编号
     * @param questionId 问卷编号
     * @return 选项确实属于该问卷返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean optionBelongsToQuestion(int optionId, int questionId) throws SQLException {
        String sql = "select count(*) from vote_options where id = ? and question_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, optionId);
            ps.setInt(2, questionId);
            rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 删除一个问卷。
     *
     * 为什么不在这里同时删除关联的选项？
     * 因为 vote_options 表设置了外键 ON DELETE CASCADE，
     * 数据库会在删除问卷时自动删除它下面的所有选项。
     * 把这项工作交给数据库更可靠，不会出现"问卷删了选项还在"的情况。
     *
     * 注意：只有通过 isQuestionOwner 检查的人才应该调用此方法。
     *
     * @param questionId 要删除的问卷编号
     * @return 成功删除返回 true（问卷不存在时返回 false）
     * @throws SQLException 数据库错误
     */
    public static boolean deleteQuestion(int questionId) throws SQLException {
        String sql = "delete from vote_questions where id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            return ps.executeUpdate() == 1;
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    // ==================== 管理员相关方法 ====================

    /**
     * 查询普通用户可见的问卷 —— 只返回已通过和已结束的问卷。
     *
     * 待审批的问卷对普通用户不可见，保证未被审批的问卷不会出现在投票列表中。
     *
     * @return 已通过和已结束状态的问卷列表
     * @throws SQLException 数据库错误
     */
    public static List<VoteQuestion> findQuestionsForUser() throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.status, q.created_at "
                + "from vote_questions q join users u on q.user_id = u.id "
                + "where q.status in ('approved', 'ended') "
                + "order by q.created_at desc";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            List<VoteQuestion> questions = new ArrayList<VoteQuestion>();
            while (rs.next()) {
                questions.add(mapQuestion(rs));
            }
            return questions;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 查询管理员可见的全部问卷 —— 包括待审批、已通过和已结束。
     *
     * 管理员需要看到所有问卷以便审批和管理。
     *
     * @return 所有状态的问卷列表
     * @throws SQLException 数据库错误
     */
    public static List<VoteQuestion> findAllQuestionsForAdmin() throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.status, q.created_at "
                + "from vote_questions q join users u on q.user_id = u.id "
                + "order by q.created_at desc";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            List<VoteQuestion> questions = new ArrayList<VoteQuestion>();
            while (rs.next()) {
                questions.add(mapQuestion(rs));
            }
            return questions;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 审批通过一个问卷 —— 将状态从 pending 改为 approved。
     *
     * 只有管理员可以调用此方法，校验在 Servlet 层完成。
     *
     * @param questionId 要审批的问卷编号
     * @return 成功更新返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean approveQuestion(int questionId) throws SQLException {
        String sql = "update vote_questions set status = 'approved' where id = ? and status = 'pending'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            return ps.executeUpdate() == 1;
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    /**
     * 结束一个问卷 —— 将状态从 approved 改为 ended。
     *
     * 结束后用户不可再投票，但结果仍然可查看。
     * 只有管理员或问卷发布者可以调用，校验在 Servlet 层完成。
     *
     * @param questionId 要结束的问卷编号
     * @return 成功更新返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean endQuestion(int questionId) throws SQLException {
        String sql = "update vote_questions set status = 'ended' where id = ? and status = 'approved'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, questionId);
            return ps.executeUpdate() == 1;
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    /**
     * 检查用户是否有权管理问卷 —— 是发布者或管理员。
     *
     * 合并了原有的发布者检查和新增的管理员检查。
     * 如果用户是发布者本人或有 admin 角色，都可以管理该问卷。
     *
     * @param questionId 问卷编号
     * @param userId     用户编号
     * @return 用户是发布者本人或管理员返回 true
     * @throws SQLException 数据库错误
     */
    public static boolean isQuestionOwnerOrAdmin(int questionId, int userId) throws SQLException {
        // 先检查是否是发布者（使用已有的轻量查询）
        if (isQuestionOwner(questionId, userId)) {
            return true;
        }
        // 不是发布者，再检查是否是管理员
        return UserModel.isAdmin(userId);
    }

    // ==================== 数据库行 → Java 对象的转换方法 ====================

    /**
     * 把 ResultSet 的当前行转换为 VoteQuestion 对象。
     */
    private static VoteQuestion mapQuestion(ResultSet rs) throws SQLException {
        VoteQuestion question = new VoteQuestion();
        question.setId(rs.getInt("id"));
        question.setTitle(rs.getString("title"));
        question.setUserId(rs.getInt("user_id"));
        question.setUsername(rs.getString("username"));
        question.setStatus(rs.getString("status"));
        question.setCreatedAt(rs.getTimestamp("created_at"));
        return question;
    }

    /**
     * 把 ResultSet 的当前行转换为 VoteOption 对象。
     *
     * 注意：这里只映射数据库里实际存在的列。
     * percent 字段不在数据库中，由 Servlet 在展示结果时计算。
     */
    private static VoteOption mapOption(ResultSet rs) throws SQLException {
        VoteOption option = new VoteOption();
        option.setId(rs.getInt("id"));
        option.setQuestionId(rs.getInt("question_id"));
        option.setContent(rs.getString("content"));
        option.setVoteCount(rs.getInt("vote_count"));
        return option;
    }
}
