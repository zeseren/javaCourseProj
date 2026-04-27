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
 * 用户控制器。
 *
 * 通过 action 参数区分登录、注册和退出，适合本课程设计的小规模功能。
 */
@WebServlet("/user")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);
        String action = request.getParameter("action");

        if ("register".equals(action)) {
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        } else if ("logout".equals(action)) {
            logout(request, response);
        } else {
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);
        String action = request.getParameter("action");

        if ("register".equals(action)) {
            register(request, response);
        } else {
            login(request, response);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));

        if (username.length() == 0 || password.length() == 0) {
            request.setAttribute("error", "用户名和密码不能为空");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = UserModel.findByUsernameAndPassword(username, password);
            if (user == null) {
                request.setAttribute("error", "用户名或密码错误");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);

            Cookie cookie = new Cookie("lastUsername", username);
            cookie.setMaxAge(7 * 24 * 60 * 60);
            cookie.setPath(cookiePath(request));
            response.addCookie(cookie);

            response.sendRedirect(request.getContextPath() + "/vote/list");
        } catch (SQLException e) {
            request.setAttribute("error", "数据库访问失败：" + e.getMessage());
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));
        String confirmPassword = trim(request.getParameter("confirmPassword"));

        if (username.length() == 0 || password.length() == 0) {
            request.setAttribute("error", "用户名和密码不能为空");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "两次输入的密码不一致");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        try {
            if (UserModel.findByUsername(username) != null) {
                request.setAttribute("error", "用户名已存在");
                request.setAttribute("username", username);
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }

            UserModel.createUser(username, password);
            request.setAttribute("message", "注册成功，请登录");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "数据库访问失败：" + e.getMessage());
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/user?action=login");
    }

    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String cookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.length() == 0 ? "/" : contextPath;
    }
}
