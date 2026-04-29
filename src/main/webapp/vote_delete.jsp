<%@ page import="org.test.model.VoteQuestion" %>
<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/context.jspf" %>
<%
    VoteQuestion question = (VoteQuestion) request.getAttribute("question");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>删除问卷 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <h1>删除问卷</h1>
        <nav class="nav">
            <a href="${ctx}/vote/list">问卷列表</a>
            <a href="${ctx}/user?action=logout">退出登录</a>
        </nav>
    </div>

    <section class="panel">
        <% if (request.getAttribute("error") != null) { %>
            <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
            <a class="button secondary" href="${ctx}/vote/list">返回列表</a>
        <% } else if (question == null) { %>
            <p class="muted">没有可删除的问卷。</p>
            <a class="button secondary" href="${ctx}/vote/list">返回列表</a>
        <% } else { %>
            <p>确定要删除问卷：<strong><%= HtmlUtil.escape(question.getTitle()) %></strong> 吗？</p>
            <p class="muted">删除后，该问卷下的所有选项和票数也会一起删除。</p>
            <form method="post" action="${ctx}/vote/delete">
                <input type="hidden" name="questionId" value="<%= question.getId() %>">
                <button class="danger" type="submit">确认删除</button>
                <a class="button secondary" href="${ctx}/vote/list">取消</a>
            </form>
        <% } %>
    </section>
</main>
</body>
</html>
