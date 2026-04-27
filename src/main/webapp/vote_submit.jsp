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
    Boolean alreadyVoted = (Boolean) request.getAttribute("alreadyVoted");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>提交投票 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <h1>提交投票</h1>
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
            <p class="muted">没有可显示的问卷。</p>
        <% } else if (Boolean.TRUE.equals(alreadyVoted)) { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="message">你已经投过这个问卷，不能重复投票。</p>
            <a class="button" href="<%= request.getContextPath() %>/vote/result?questionId=<%= question.getId() %>">查看结果</a>
        <% } else { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="muted">请选择一个选项后提交。</p>

            <form method="post" action="<%= request.getContextPath() %>/vote/submit">
                <input type="hidden" name="questionId" value="<%= question.getId() %>">
                <% for (VoteOption option : options) { %>
                    <label class="option">
                        <input type="radio" name="optionId" value="<%= option.getId() %>">
                        <%= HtmlUtil.escape(option.getContent()) %>
                    </label>
                <% } %>
                <button type="submit">提交投票</button>
            </form>
        <% } %>
    </section>
</main>
</body>
</html>
