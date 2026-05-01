<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.test.model.VoteQuestion" %>
<%@ page import="org.test.model.VoteOption" %>
<%@ page import="org.test.util.HtmlUtil" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/context.jspf" %>
<%
    VoteQuestion question = (VoteQuestion) request.getAttribute("question");
    List<VoteOption> options = (List<VoteOption>) request.getAttribute("options");
    if (options == null) {
        options = new ArrayList<VoteOption>();
    }
    Boolean alreadyVoted = (Boolean) request.getAttribute("alreadyVoted");
    String status = (String) request.getAttribute("status");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>提交投票 - 校园问卷投票系统</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<main class="page">
    <div class="topbar">
        <h1>提交投票</h1>
        <nav class="nav">
            <a href="${ctx}/vote/list">问卷列表</a>
            <a href="${ctx}/user?action=logout">退出登录</a>
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
            <a class="button" href="${ctx}/vote/result?questionId=<%= question.getId() %>">查看结果</a>
            <a class="button secondary" href="${ctx}/vote/list">返回列表</a>
        <% } else if ("ended".equals(status)) { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="message">该问卷已经结束，不再接受投票。</p>
            <a class="button" href="${ctx}/vote/result?questionId=<%= question.getId() %>">查看结果</a>
            <a class="button secondary" href="${ctx}/vote/list">返回列表</a>
        <% } else if ("pending".equals(status)) { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="message">该问卷正在等待管理员审批，审批通过后才可投票。</p>
            <a class="button secondary" href="${ctx}/vote/list">返回列表</a>
        <% } else { %>
            <h1><%= HtmlUtil.escape(question.getTitle()) %></h1>
            <p class="muted">请选择一个选项后提交。</p>

            <form method="post" action="${ctx}/vote/submit">
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
