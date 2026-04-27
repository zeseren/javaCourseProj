<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String title = (String) request.getAttribute("title");
    if (title == null) {
        title = "";
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>发布问卷 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <h1>发布问卷</h1>
        <nav class="nav">
            <a href="<%= request.getContextPath() %>/vote/list">问卷列表</a>
            <a href="<%= request.getContextPath() %>/user?action=logout">退出登录</a>
        </nav>
    </div>

    <section class="panel">
        <% if (request.getAttribute("error") != null) { %>
            <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" action="<%= request.getContextPath() %>/vote/publish">
            <div class="form-row">
                <label for="title">问卷标题</label>
                <input id="title" type="text" name="title" value="<%= HtmlUtil.escape(title) %>">
            </div>
            <div class="form-row">
                <label>投票选项</label>
                <input type="text" name="options" placeholder="选项一">
            </div>
            <div class="form-row">
                <input type="text" name="options" placeholder="选项二">
            </div>
            <div class="form-row">
                <input type="text" name="options" placeholder="选项三，可选">
            </div>
            <div class="form-row">
                <input type="text" name="options" placeholder="选项四，可选">
            </div>
            <button type="submit">发布问卷</button>
        </form>
    </section>
</main>
</body>
</html>
