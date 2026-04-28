package org.test.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC 工具类 —— 数据库连接的"水龙头"。
 *
 * 为什么要把数据库连接信息集中放在一个类里？
 * ──────────────────────────────────────────
 * 假设数据库密码写在了 10 个不同的文件中。
 * 当更换数据库、修改密码或切换环境时，
 * 你需要找到这 10 个文件逐一修改，漏一个就全崩了。
 *
 * 集中管理后，只需要改这一个地方，所有用到数据库的代码自动生效。
 *
 * 生活中的类比：
 * 整栋楼的水管不需要每个房间一个总阀 ——
 * 一个总阀门就能控制整栋楼的供水。
 *
 *
 * 连接泄漏的严重性：
 * ─────────────────
 * 数据库连接是一种有限资源，就像停车位。
 * 如果每次用完不"还回去"（close），新的请求来时就分不到连接，
 * 所有用户都会看到"数据库连接失败"。
 * 所以这里提供了 close() 方法，并配合 finally 保证一定执行。
 */
public class JdbcUtil {

    /**
     * 数据库连接地址。
     *
     * jdbc:postgresql:// ... 是 PostgreSQL 专用格式。
     * 如果换成 MySQL，这里要改成 jdbc:mysql:// ...
     * localhost:5432 —— 本地机器，端口 5432（PostgreSQL 默认端口）。
     * campus_vote —— 数据库名称。
     */
    private static final String URL = "jdbc:postgresql://localhost:5432/campus_vote";

    /**
     * 数据库用户名。
     *
     * 课程设计中使用 postgres 超级用户。
     * 真实项目中应该创建权限最小化的专用用户，
     * 比如只给 campus_vote 数据库的读写权限。
     */
    private static final String USERNAME = "postgres";

    /**
     * 数据库密码。
     *
     * 课程设计中为简单起见明文写在这里。
     * 真实项目中密码应该从环境变量或配置文件读取，
     * 绝不能硬编码在代码中，更不能提交到 Git。
     */
    private static final String PASSWORD = "123456";

    /**
     * 类加载时自动注册 PostgreSQL 驱动。
     *
     * static { ... } 是静态初始化块，
     * 在这个类第一次被用到时自动执行。
     *
     * Class.forName(...) 的作用：
     * 加载 PostgreSQL 驱动的类，驱动内部会向 JDK 注册自己。
     * 不注册驱动，DriverManager 就不知道如何处理
     * "jdbc:postgresql://..." 这种连接地址。
     *
     * 为什么把异常包装成 RuntimeException？
     * 驱动加载失败是"没法继续运行"级别的错误，
     * 与其让每个调用方都处理，不如直接终止程序。
     */
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC 驱动加载失败", e);
        }
    }

    /**
     * 私有构造方法，防止误用。
     * 这个类的所有方法都是 static，不需要创建实例。
     */
    private JdbcUtil() {
    }

    /**
     * 获取一个数据库连接。
     *
     * DriverManager 会从连接池或直接创建新连接返回。
     * 调用方用完必须调用 close() 归还连接！
     *
     * @return 一个可用的数据库连接
     * @throws SQLException 连不上数据库时抛出（数据库没启动、密码错等）
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * 安全关闭 ResultSet、PreparedStatement 和 Connection。
     *
     * 为什么每个 close 都要套 try-catch？
     * 因为 close() 本身可能抛出 SQLException。
     * 如果第一个 close 抛异常，没有 try-catch 的话
     * 后面的 close 就不会执行了，导致连接泄漏。
     *
     * 为什么 catch 里什么都不做？
     * 关闭时报错通常是因为连接早就断了，
     * 这种错误不影响程序继续运行，记录日志即可（课程设计中省略）。
     *
     * @param rs ResultSet（可为 null）
     * @param ps PreparedStatement（可为 null）
     * @param conn Connection（可为 null）
     */
    public static void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
                // 关闭 ResultSet 时出错，不影响后续关闭操作
            }
        }
        // 复用两参数版本的 close 来关闭 ps 和 conn
        close(ps, conn);
    }

    /**
     * 安全关闭 PreparedStatement 和 Connection。
     *
     * 关的顺序有讲究吗？
     * 有。ResultSet → PreparedStatement → Connection 是推荐顺序。
     * 不过现代 JDBC 驱动通常能自动处理乱序关闭，
     * 所以这里的顺序影响不大。
     *
     * 为什么允许传 null？
     * 为了在 finally 块中调用方便 —— 不需要先判空。
     * 如果某一步初始化失败了（比如 getConnection 抛异常），
     * ps 可能还是 null，这时候直接调 close 会空指针异常。
     *
     * @param ps   PreparedStatement（可为 null）
     * @param conn Connection（可为 null）
     */
    public static void close(PreparedStatement ps, Connection conn) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException ignored) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
