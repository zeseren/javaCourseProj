<%@ page import="jakarta.servlet.http.Cookie" %>
<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/context.jspf" %>
<%
    String username = (String) request.getAttribute("username");
    if (username == null) {
        username = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("lastUsername".equals(cookie.getName())) {
                    username = cookie.getValue();
                    break;
                }
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<main class="page">
    <section class="panel">
        <h1>校园问卷投票系统</h1>
        <p class="muted">请登录后发布问卷或参与投票。</p>

        <% if (request.getAttribute("message") != null) { %>
            <div class="message"><%= HtmlUtil.escape(request.getAttribute("message")) %></div>
        <% } %>
        <% if (request.getAttribute("error") != null) { %>
            <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" action="${ctx}/user?action=login">
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" value="<%= HtmlUtil.escape(username) %>">
            </div>
            <div class="form-row">
                <label for="password">密码</label>
                <input id="password" type="password" name="password">
            </div>
            <button type="submit">登录</button>
            <a class="button secondary" href="${ctx}/user?action=register">注册新用户</a>
        </form>
    </section>
</main>
</body>
</html>
