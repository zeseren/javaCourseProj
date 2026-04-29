<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/context.jspf" %>
<%
    String username = (String) request.getAttribute("username");
    if (username == null) {
        username = "";
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>注册 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<main class="page">
    <section class="panel">
        <h1>注册用户</h1>
        <p class="muted">注册后即可发布问卷和参与投票。</p>

        <% if (request.getAttribute("error") != null) { %>
            <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" action="${ctx}/user?action=register">
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" value="<%= HtmlUtil.escape(username) %>">
            </div>
            <div class="form-row">
                <label for="password">密码</label>
                <input id="password" type="password" name="password">
            </div>
            <div class="form-row">
                <label for="confirmPassword">确认密码</label>
                <input id="confirmPassword" type="password" name="confirmPassword">
            </div>
            <button type="submit">注册</button>
            <a class="button secondary" href="${ctx}/user?action=login">返回登录</a>
        </form>
    </section>
</main>
</body>
</html>
