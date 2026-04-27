<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    if (session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/user?action=login");
    } else {
        response.sendRedirect(request.getContextPath() + "/vote/list");
    }
%>
