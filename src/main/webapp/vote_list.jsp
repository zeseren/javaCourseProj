<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.test.model.User" %>
<%@ page import="org.test.model.VoteQuestion" %>
<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/context.jspf" %>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) {
        currentUser = (User) session.getAttribute("currentUser");
    }
    List<VoteQuestion> questions = (List<VoteQuestion>) request.getAttribute("questions");
    if (questions == null) {
        questions = new ArrayList<VoteQuestion>();
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>问卷列表 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <div>
            <h1>问卷列表</h1>
            <p class="muted">当前用户：<%= HtmlUtil.escape(currentUser == null ? "" : currentUser.getUsername()) %></p>
        </div>
        <nav class="nav">
            <a class="button" href="${ctx}/vote/publish">发布问卷</a>
            <a href="${ctx}/user?action=logout">退出登录</a>
        </nav>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
    <% } %>

    <section class="panel">
        <% if (questions.isEmpty()) { %>
            <p class="muted">暂时没有问卷，请先发布一个问卷。</p>
        <% } else { %>
            <table class="table">
                <thead>
                <tr>
                    <th>标题</th>
                    <th>发布者</th>
                    <th>发布时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% for (VoteQuestion question : questions) { %>
                    <tr>
                        <td><%= HtmlUtil.escape(question.getTitle()) %></td>
                        <td><%= HtmlUtil.escape(question.getUsername()) %></td>
                        <td><%= HtmlUtil.escape(question.getCreatedAt()) %></td>
                        <td>
                            <div class="actions">
                                <a href="${ctx}/vote/submit?questionId=<%= question.getId() %>">去投票</a>
                                <a href="${ctx}/vote/result?questionId=<%= question.getId() %>">看结果</a>
                                <% if (currentUser != null && currentUser.getId() == question.getUserId()) { %>
                                    <a href="${ctx}/vote/delete?questionId=<%= question.getId() %>">删除</a>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        <% } %>
    </section>
</main>
</body>
</html>
