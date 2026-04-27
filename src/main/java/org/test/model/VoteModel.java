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
 * 投票数据访问方法。
 *
 * 这里集中处理问卷、选项和票数相关 SQL。Servlet 只关心业务流程，
 * 不需要知道每条 SQL 具体怎么写。
 */
public class VoteModel {
    private VoteModel() {
    }

    public static boolean createQuestion(int userId, String title, List<String> options) throws SQLException {
        String questionSql = "insert into vote_questions(title, user_id) values(?, ?)";
        String optionSql = "insert into vote_options(question_id, content) values(?, ?)";
        Connection conn = null;
        PreparedStatement questionPs = null;
        PreparedStatement optionPs = null;
        ResultSet keys = null;
        try {
            conn = JdbcUtil.getConnection();
            conn.setAutoCommit(false);

            questionPs = conn.prepareStatement(questionSql, Statement.RETURN_GENERATED_KEYS);
            questionPs.setString(1, title);
            questionPs.setInt(2, userId);
            questionPs.executeUpdate();

            keys = questionPs.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                return false;
            }

            int questionId = keys.getInt(1);
            optionPs = conn.prepareStatement(optionSql);
            for (String option : options) {
                optionPs.setInt(1, questionId);
                optionPs.setString(2, option);
                optionPs.addBatch();
            }
            optionPs.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (keys != null) {
                try {
                    keys.close();
                } catch (SQLException ignored) {
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

    public static List<VoteQuestion> findAllQuestions() throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.created_at "
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

    public static VoteQuestion findQuestionById(int questionId) throws SQLException {
        String sql = "select q.id, q.title, q.user_id, u.username, q.created_at "
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

    private static VoteQuestion mapQuestion(ResultSet rs) throws SQLException {
        VoteQuestion question = new VoteQuestion();
        question.setId(rs.getInt("id"));
        question.setTitle(rs.getString("title"));
        question.setUserId(rs.getInt("user_id"));
        question.setUsername(rs.getString("username"));
        question.setCreatedAt(rs.getTimestamp("created_at"));
        return question;
    }

    private static VoteOption mapOption(ResultSet rs) throws SQLException {
        VoteOption option = new VoteOption();
        option.setId(rs.getInt("id"));
        option.setQuestionId(rs.getInt("question_id"));
        option.setContent(rs.getString("content"));
        option.setVoteCount(rs.getInt("vote_count"));
        return option;
    }
}
