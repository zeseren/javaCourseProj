package org.test.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.test.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserModel 集成测试。
 *
 * 这些测试需要连接真实的 PostgreSQL 数据库（campus_vote）。
 * 每个测试方法运行前会清理上一次残留的测试数据，
 * 运行后也会删除本次产生的测试数据，确保不影响其他用户。
 *
 * 运行前请确认：
 * 1. PostgreSQL 已启动
 * 2. campus_vote 数据库和 users 表已创建
 */
class UserModelTest {

    // 测试专用用户名前缀，避免和真实用户冲突
    private static final String TEST_USER = "_test_user_model_";

    @BeforeEach
    void setUp() throws SQLException {
        // 删除上一轮可能残留的测试数据
        deleteTestUser();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // 清理本轮测试数据
        deleteTestUser();
    }

    private void deleteTestUser() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement("delete from users where username like ?");
            ps.setString(1, TEST_USER + "%");
            ps.executeUpdate();
        } finally {
            JdbcUtil.close(ps, conn);
        }
    }

    @Test
    void shouldFindUserByUsername() throws SQLException {
        // 先创建一个测试用户
        UserModel.createUser(TEST_USER + "find", "pass123");

        // 用 findByUsername 查询
        User user = UserModel.findByUsername(TEST_USER + "find");

        assertNotNull(user);
        assertEquals(TEST_USER + "find", user.getUsername());
        assertEquals("pass123", user.getPassword());
        assertTrue(user.getId() > 0);
    }

    @Test
    void shouldReturnNullWhenUserNotFound() throws SQLException {
        User user = UserModel.findByUsername(TEST_USER + "nonexistent");
        assertNull(user);
    }

    @Test
    void shouldFindByUsernameAndPassword() throws SQLException {
        UserModel.createUser(TEST_USER + "login", "mypassword");

        // 正确的用户名和密码应该返回用户
        User user = UserModel.findByUsernameAndPassword(TEST_USER + "login", "mypassword");
        assertNotNull(user);
        assertEquals(TEST_USER + "login", user.getUsername());
    }

    @Test
    void shouldReturnNullWhenPasswordWrong() throws SQLException {
        UserModel.createUser(TEST_USER + "wrongpwd", "correct");

        // 密码不对应该返回 null
        User user = UserModel.findByUsernameAndPassword(TEST_USER + "wrongpwd", "wrong");
        assertNull(user);
    }

    @Test
    void shouldReturnNullWhenUsernameWrong() throws SQLException {
        UserModel.createUser(TEST_USER + "real", "realpass");

        // 用户名不对应该返回 null
        User user = UserModel.findByUsernameAndPassword(TEST_USER + "fake", "realpass");
        assertNull(user);
    }

    @Test
    void shouldCreateUserSuccessfully() throws SQLException {
        boolean created = UserModel.createUser(TEST_USER + "create", "newpass");
        assertTrue(created);

        // 验证数据确实写入了
        User user = UserModel.findByUsername(TEST_USER + "create");
        assertNotNull(user);
    }
}
