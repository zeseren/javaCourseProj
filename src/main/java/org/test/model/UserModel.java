package org.test.model;

import org.test.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用户数据访问层 —— 负责所有和 users 表相关的数据库操作。
 *
 * 这个类存在的意义：
 * ──────────────────
 * 如果把 SQL 直接写在 Servlet 里，会出现两个问题：
 * 1. 同样的 SQL 可能出现在多个地方，改一处漏一处
 * 2. Servlet 的本职工作是"处理网页请求"，再塞进 SQL 会让代码混乱
 *
 * 所以把数据库操作集中放在这里。Servlet 只需要调用
 * UserModel.findByUsername("张三")，不需要知道 SQL 怎么写。
 *
 * 为什么方法都是 static？
 * 这个类没有任何需要保存的状态（没有字段），
 * 每个方法都是"输入 → 查数据库 → 返回结果"的独立过程。
 * 用 static 方法最简单直接，不需要先 new 一个对象。
 *
 * 为什么构造方法是 private？
 * 纯工具性质的类不需要被实例化，把构造方法藏起来
 * 可以防止别人意外地 new UserModel()。
 */
public class UserModel {

    // 私有构造方法：这个类不需要被 new
    private UserModel() {
    }

    /**
     * 根据用户名查找用户。
     *
     * 使用场景：注册时检查用户名是否已被占用。
     * 因为只需要用户名一个条件，所以只传 username 就够了。
     *
     * 为什么返回 null 而不是直接报错？
     * "查不到"不是异常情况（比如注册时发现用户名可用），
     * 返回 null 让调用方自己决定怎么处理更灵活。
     *
     * @param username 要查找的用户名
     * @return 找到则返回 User 对象，找不到返回 null
     * @throws SQLException 数据库连接失败或 SQL 语法错误时抛出
     */
    public static User findByUsername(String username) throws SQLException {
        // SQL 中只查需要的列，避免 SELECT * 拖慢查询
        String sql = "select id, username, password, created_at from users where username = ?";

        // JDBC 的固定流程：获取连接 → 准备 SQL → 填参数 → 执行 → 处理结果
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            // ? 占位符从 1 开始编号，不是从 0 开始
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                // ResultSet 当前指向的行里有数据，把它转换成 User 对象
                return mapUser(rs);
            }
            // 没查到任何行，说明这个用户名不在数据库中
            return null;
        } finally {
            // finally 保证无论是否出错，连接都会被释放
            // 否则连接池会被耗尽，导致整个系统卡死
            JdbcUtil.close(rs, ps, conn);
        }
    }

    /**
     * 登录验证 —— 同时检查用户名和密码是否匹配。
     *
     * 为什么不用两步（先查用户名，再比密码）？
     * 一次 SQL 查完效率更高，数据库在 WHERE 条件里就能完成判断。
     * 而且从安全角度看，不要在代码里把密码读出来再比较 ——
     * 把比较交给数据库，密码不会在 Java 内存中多停留。
     * （当然，当前是明文密码，这只是为了课程演示的简化做法）
     *
     * @param username 登录时输入的用户名
     * @param password 登录时输入的密码
     * @return 匹配成功返回 User，失败返回 null
     * @throws SQLException 数据库错误
     */
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

    /**
     * 注册新用户 —— 向 users 表插入一条记录。
     *
     * 为什么返回 boolean 而不是新用户的完整信息？
     * 注册成功后直接引导用户去登录，不需要完整的 User 对象。
     * 返回 true/false 足够判断操作是否成功。
     *
     * 注意：当前没有对密码做任何加密处理。
     * 真实项目中必须在入库前用 bcrypt/scrypt 对密码做哈希。
     *
     * @param username 注册用户名
     * @param password 注册密码
     * @return 插入了一行则返回 true
     * @throws SQLException 数据库错误（如用户名重复导致唯一约束冲突）
     */
    public static boolean createUser(String username, String password) throws SQLException {
        String sql = "insert into users(username, password) values(?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            // executeUpdate 返回受影响的行数，1 表示成功插入了一行
            return ps.executeUpdate() == 1;
        } finally {
            // 这里不需要关 ResultSet，因为 INSERT 不产生结果集
            JdbcUtil.close(ps, conn);
        }
    }

    /**
     * 把数据库的一行记录转换成 Java 的 User 对象。
     *
     * 为什么单独抽一个方法？
     * 上面三个方法都需要做同样的转换工作，
     * 写成一个方法可以避免重复代码。
     * 将来如果 users 表加了新字段，也只需要改这一处。
     */
    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
