package org.test.model;

import org.test.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用户数据访问方法。
 *
 * 本项目省略复杂 DAO 分层，但仍然把 SQL 放在 Model 辅助类中，
 * 避免 Servlet 直接堆数据库代码。
 */
public class UserModel {
    private UserModel() {
    }

    public static User findByUsername(String username) throws SQLException {
        String sql = "select id, username, password, created_at from users where username = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
            return null;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    public static User findByUsernameAndPassword(String username, String password) throws SQLException {
        String sql = "select id, username, password, created_at from users where username = ? and password = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
            return null;
        } finally {
            JdbcUtil.close(rs, ps, conn);
        }
    }

    public static boolean createUser(String username, String password) throws SQLException {
        String sql = "insert into users(username, password) values(?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            return ps.executeUpdate() == 1;
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
