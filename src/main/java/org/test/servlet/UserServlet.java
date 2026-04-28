package org.test.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.test.model.User;
import org.test.model.UserModel;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 用户控制器 —— 处理登录、注册和退出。
 *
 * 这个 Servlet 是整个系统的"大门"。
 * 所有未登录的用户都会被引导到这里。
 * 登录成功后，用户的身份信息保存在 Session 中，
 * 后续访问任何页面都可以通过 Session 知道"是谁在操作"。
 *
 * ============ WebServlet 注解 ============
 *
 * @WebServlet("/user")
 * 这行注解告诉 Tomcat：
 *   "如果有人访问 /user 这个地址，就交给这个类来处理。"
 *
 * 为什么不用 web.xml 配置？
 * 注解更简洁，配置和代码放在一起不容易遗漏。
 * 对于这种小规模项目，注解足够了。
 *
 *
 * ============ GET vs POST ============
 *
 * doGet:  用户在浏览器地址栏输入网址、点击链接时触发。
 *         用于展示页面（登录页、注册页）和执行退出。
 *
 * doPost: 用户提交表单时触发。
 *         用于处理登录和注册的表单数据。
 *
 * 为什么登录/注册用 POST 而不是 GET？
 * 因为 GET 会把表单数据（包括密码）暴露在浏览器地址栏里。
 * POST 把数据放在请求体中，虽然不加密，但至少不直接显示在 URL 里。
 *
 *
 * ============ Session 和 Cookie ============
 *
 * Session（会话）：
 *   服务器端存储的一块内存，用来记住"当前用户是谁"。
 *   用户登录后，把 User 对象放进 Session。
 *   后续请求都能从 Session 中取出这个 User，不需要重新登录。
 *   关闭浏览器一段时间后 Session 自动过期。
 *
 * Cookie（小甜饼）：
 *   浏览器端存储的一小段数据。
 *   这里用来记住"上次登录的用户名"，方便用户下次登录时自动填充。
 *   设置了 7 天有效期（7 × 24 × 60 × 60 秒）。
 */
@WebServlet("/user")
public class UserServlet extends HttpServlet {

    /**
     * 处理 GET 请求 —— 展示页面或退出。
     *
     * action 参数的三种情况：
     *   register → 展示注册页面
     *   logout   → 执行退出操作
     *   其他值   → 默认展示登录页面
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 第一步永远是设置编码，否则中文会变成乱码
        prepareEncoding(request, response);

        String action = request.getParameter("action");

        if ("register".equals(action)) {
            // 转发到注册页面（URL 不变，用户看到的是 register.jsp 的内容）
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        } else if ("logout".equals(action)) {
            // 退出：清除 Session，跳回登录页
            logout(request, response);
        } else {
            // 默认：展示登录页面
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    /**
     * 处理 POST 请求 —— 提交登录或注册表单。
     *
     * 为什么不在 doGet 里处理表单提交？
     * HTTP 规范规定 GET 用于获取数据，POST 用于提交数据。
     * 遵守规范能让浏览器、代理服务器正确处理缓存和重放。
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        String action = request.getParameter("action");

        if ("register".equals(action)) {
            register(request, response);
        } else {
            // POST 的其他情况都当作登录处理
            login(request, response);
        }
    }

    // ==================== 登录逻辑 ====================

    /**
     * 处理登录表单提交。
     *
     * 登录的完整流程：
     *   1. 读取用户名和密码（去掉首尾空格）
     *   2. 检查是否为空 → 空就直接返回错误提示
     *   3. 去数据库查询匹配的用户
     *   4. 找不到 → 用户名或密码错误
     *   5. 找到了 → 把用户信息存入 Session，种下记住用户名的 Cookie
     *   6. 重定向到问卷列表页
     *
     * 为什么登录成功后用 redirect 而不是 forward？
     * forward 会导致浏览器地址栏还停留在 /user 上。
     * 用户刷新页面时，浏览器会重新提交登录表单，
     * 可能导致重复登录甚至报错。
     * redirect 让浏览器跳转到新地址，刷新时只刷新列表页。
     */
    private void login(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));

        // 非空校验 —— 在查数据库之前先做，节省数据库资源
        if (username.length() == 0 || password.length() == 0) {
            request.setAttribute("error", "用户名和密码不能为空");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = UserModel.findByUsernameAndPassword(username, password);
            if (user == null) {
                // 故意不告诉用户是"用户名错了"还是"密码错了"
                // 这样攻击者无法通过错误提示来枚举有哪些用户名
                request.setAttribute("error", "用户名或密码错误");
                request.setAttribute("username", username); // 保留已输入的用户名，方便修改
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            // 登录成功：把用户信息存到 Session 中
            // Session 是服务器端的存储，每个登录用户都有自己独立的 Session
            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);

            // 种一个 Cookie 记住用户名，方便下次登录
            // Cookie 存在浏览器端，7 天后自动删除
            Cookie cookie = new Cookie("lastUsername", username);
            cookie.setMaxAge(7 * 24 * 60 * 60); // 秒数：7天
            cookie.setPath(cookiePath(request));  // 设置为整个应用范围可见
            response.addCookie(cookie);

            // 重定向到问卷列表页（浏览器地址栏会变成 /vote/list）
            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "数据库访问失败：" + e.getMessage());
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    // ==================== 注册逻辑 ====================

    /**
     * 处理注册表单提交。
     *
     * 注册校验的三道防线：
     *   1. 非空检查 —— 用户名和密码不能是空白
     *   2. 一致性检查 —— 两次输入的密码必须相同
     *   3. 唯一性检查 —— 用户名不能已经被占用
     *
     * 为什么这三道防线都在服务器端再做一次？
     * 因为浏览器端的 JS 校验可以被用户绕过（禁用 JS 或用工具直接发请求）。
     * 服务器端校验是最后的安全保障。
     *
     * 注册成功后为什么不自动登录？
     * 这是课程设计的简化做法 —— 让用户手动登录一次，
     * 可以清楚地体验"注册"和"登录"是两个独立的操作。
     */
    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));
        String confirmPassword = trim(request.getParameter("confirmPassword"));

        // 第一道防线：非空
        if (username.length() == 0 || password.length() == 0) {
            request.setAttribute("error", "用户名和密码不能为空");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // 第二道防线：两次密码一致
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "两次输入的密码不一致");
            request.setAttribute("username", username); // 保留用户名，让用户不用重输
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        try {
            // 第三道防线：用户名唯一
            if (UserModel.findByUsername(username) != null) {
                request.setAttribute("error", "用户名已存在");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }

            // 三道防线都通过，写入数据库
            UserModel.createUser(username, password);

            // 注册成功 → 跳转到登录页，提示用户登录
            request.setAttribute("message", "注册成功，请登录");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "数据库访问失败：" + e.getMessage());
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }

    // ==================== 退出逻辑 ====================

    /**
     * 退出登录 —— 清除 Session 并跳回登录页。
     *
     * request.getSession(false) 和 getSession() 的区别：
     *   getSession(false) → 没有 Session 就返回 null，不创建新的
     *   getSession()      → 没有 Session 就创建一个新的
     *
     * 退出时用 false：如果没有 Session 就什么都不用做。
     *
     * invalidate() 的作用：
     * 标记 Session 为无效，服务器随后会回收它占用的内存。
     * 用户再访问时，服务器发现没有有效 Session → 要求重新登录。
     */
    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/user?action=login");
    }

    // ==================== 辅助小工具 ====================

    /**
     * 统一设置请求和响应的字符编码。
     *
     * 为什么三行都要写？
     * - setCharacterEncoding("UTF-8")：Tomcat 读取请求参数时用 UTF-8 解码
     * - setCharacterEncoding("UTF-8") + setContentType：浏览器知道用 UTF-8 显示
     *
     * 如果不设置编码，Tomcat 默认用 ISO-8859-1 解码中文，
     * 导致所有中文字符变成乱码（???）。
     */
    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }

    /**
     * 安全地去除字符串首尾空格。
     *
     * 为什么单独抽一个方法？
     * request.getParameter() 可能返回 null（参数不存在时）。
     * 直接在 null 上调用 trim() 会抛 NullPointerException。
     * 用这个方法统一处理，避免到处写判空逻辑。
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 获取 Cookie 的生效路径。
     *
     * 为什么要设置 Cookie 的 path？
     * 如果不设置，Cookie 默认只在当前路径下可见（/user）。
     * 那访问 /vote/list 时就拿不到这个 Cookie 了。
     *
     * 设置为应用根路径（如 /javaCourseProj），
     * 确保整个应用范围内的页面都能读取到这个 Cookie。
     *
     * 特殊处理：如果应用部署在根路径，
     * getContextPath() 返回空字符串，此时用 "/"。
     */
    private String cookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.length() == 0 ? "/" : contextPath;
    }
}
