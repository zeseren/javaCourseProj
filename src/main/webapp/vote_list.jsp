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
    boolean isAdmin = currentUser != null && "admin".equals(currentUser.getRole());

    List<VoteQuestion> questions = (List<VoteQuestion>) request.getAttribute("questions");
    if (questions == null) {
        questions = new ArrayList<VoteQuestion>();
    }

    // 从 Session 中读取闪存消息（显示一次后清除）
    String flashMessage = (String) session.getAttribute("message");
    if (flashMessage != null) {
        session.removeAttribute("message");
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
            <p class="muted">
                当前用户：<%= HtmlUtil.escape(currentUser == null ? "" : currentUser.getUsername()) %>
                <% if (isAdmin) { %>
                    <span class="badge admin">管理员</span>
                <% } %>
            </p>
        </div>
        <nav class="nav">
            <a class="button" href="${ctx}/vote/publish">发布问卷</a>
            <a href="${ctx}/user?action=logout">退出登录</a>
        </nav>
    </div>

    <%-- 闪存提示消息（发布成功、操作成功等） --%>
    <% if (flashMessage != null) { %>
        <div class="message"><%= HtmlUtil.escape(flashMessage) %></div>
    <% } %>

    <% if (request.getAttribute("error") != null) { %>
        <div class="error"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
    <% } %>

    <%-- ==================== 管理员视图：待审批问卷 ==================== --%>
    <%
        // 从所有问卷中筛选出不同状态的
        List<VoteQuestion> pendingQuestions = new ArrayList<VoteQuestion>();
        List<VoteQuestion> approvedQuestions = new ArrayList<VoteQuestion>();
        List<VoteQuestion> endedQuestions = new ArrayList<VoteQuestion>();
        for (VoteQuestion q : questions) {
            if ("pending".equals(q.getStatus())) {
                pendingQuestions.add(q);
            } else if ("ended".equals(q.getStatus())) {
                endedQuestions.add(q);
            } else {
                approvedQuestions.add(q);
            }
        }
    %>

    <%-- 管理员：待审批区域 --%>
    <% if (isAdmin && !pendingQuestions.isEmpty()) { %>
        <section class="panel pending-section">
            <h2>待审批问卷（<%= pendingQuestions.size() %>）</h2>
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
                <% for (VoteQuestion q : pendingQuestions) { %>
                    <tr>
                        <td><%= HtmlUtil.escape(q.getTitle()) %></td>
                        <td><%= HtmlUtil.escape(q.getUsername()) %></td>
                        <td><%= HtmlUtil.escape(q.getCreatedAt()) %></td>
                        <td>
                            <div class="actions">
                                <form method="post" action="${ctx}/admin" style="display: inline;">
                                    <input type="hidden" name="action" value="approve">
                                    <input type="hidden" name="questionId" value="<%= q.getId() %>">
                                    <button type="submit" class="link-button approve-btn">审批通过</button>
                                </form>
                                <a href="${ctx}/vote/result?questionId=<%= q.getId() %>">看结果</a>
                                <a href="${ctx}/vote/delete?questionId=<%= q.getId() %>" class="danger-link">删除</a>
                            </div>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </section>
    <% } %>

    <%-- ==================== 已通过问卷 ==================== --%>
    <section class="panel">
        <h2>
            <%= isAdmin ? "已通过问卷" : "可投票问卷" %>
            <% if (!approvedQuestions.isEmpty()) { %>
                <span class="count">（<%= approvedQuestions.size() %>）</span>
            <% } %>
        </h2>
        <% if (approvedQuestions.isEmpty()) { %>
            <p class="muted">暂时没有可投票的问卷。</p>
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
                <% for (VoteQuestion q : approvedQuestions) { %>
                    <tr>
                        <td><%= HtmlUtil.escape(q.getTitle()) %></td>
                        <td><%= HtmlUtil.escape(q.getUsername()) %></td>
                        <td><%= HtmlUtil.escape(q.getCreatedAt()) %></td>
                        <td>
                            <div class="actions">
                                <a href="${ctx}/vote/submit?questionId=<%= q.getId() %>">去投票</a>
                                <a href="${ctx}/vote/result?questionId=<%= q.getId() %>">看结果</a>
                                <%-- 管理员或发布者：可以结束和删除 --%>
                                <% if (isAdmin || currentUser.getId() == q.getUserId()) { %>
                                    <form method="post" action="${ctx}/admin" style="display: inline;">
                                        <input type="hidden" name="action" value="end">
                                        <input type="hidden" name="questionId" value="<%= q.getId() %>">
                                        <button type="submit" class="link-button end-btn">结束</button>
                                    </form>
                                    <a href="${ctx}/vote/delete?questionId=<%= q.getId() %>" class="danger-link">删除</a>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        <% } %>
    </section>

    <%-- ==================== 已结束问卷 ==================== --%>
    <% if (!endedQuestions.isEmpty()) { %>
        <section class="panel ended-section">
            <h2>已结束问卷（<%= endedQuestions.size() %>）</h2>
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
                <% for (VoteQuestion q : endedQuestions) { %>
                    <tr>
                        <td>
                            <%= HtmlUtil.escape(q.getTitle()) %>
                            <span class="badge ended">已结束</span>
                        </td>
                        <td><%= HtmlUtil.escape(q.getUsername()) %></td>
                        <td><%= HtmlUtil.escape(q.getCreatedAt()) %></td>
                        <td>
                            <div class="actions">
                                <a href="${ctx}/vote/result?questionId=<%= q.getId() %>">看结果</a>
                                <%-- 管理员或发布者：可以删除 --%>
                                <% if (isAdmin || currentUser.getId() == q.getUserId()) { %>
                                    <a href="${ctx}/vote/delete?questionId=<%= q.getId() %>" class="danger-link">删除</a>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </section>
    <% } %>

    <%-- 没有任何问卷时 --%>
    <% if (questions.isEmpty()) { %>
        <p class="muted">暂时没有问卷，请先发布一个问卷。</p>
    <% } %>
</main>
</body>
</html>
