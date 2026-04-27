<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.test.model.VoteQuestion" %>
<%@ page import="org.test.model.VoteOption" %>
<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    VoteQuestion question = (VoteQuestion) request.getAttribute("question");
    List<VoteOption> options = (List<VoteOption>) request.getAttribute("options");
    if (options == null) {
        options = new ArrayList<VoteOption>();
    }
    Integer totalVotes = (Integer) request.getAttribute("totalVotes");
    if (totalVotes == null) {
        totalVotes = Integer.valueOf(0);
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>投票结果 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <h1>投票结果</h1>
        <nav class="nav">
            <a href="<%= request.getContextPath() %>/vote/list">问卷列表</a>
            <a href="<%= request.getContextPath() %>/user?action=logout">退出登录</a>
        </nav>
    </div>

    <section class="panel">
        <% if (request.getAttribute("error") != null) { %>
            <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>

        <% if (question == null) { %>
            <p class="muted">没有可显示的结果。</p>
        <% } else { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="muted">总票数：<%= totalVotes.intValue() %></p>

            <% for (VoteOption option : options) { %>
                <div class="result-row">
                    <div class="result-line">
                        <strong><%= HtmlUtil.escape(option.getContent()) %></strong>
                        <span><%= option.getVoteCount() %> 票，<%= String.format("%.1f", option.getPercent()) %>%</span>
                    </div>
                    <div class="bar">
                        <span style="width: <%= option.getPercent() %>%;"></span>
                    </div>
                </div>
            <% } %>
        <% } %>
    </section>
</main>
</body>
</html>
