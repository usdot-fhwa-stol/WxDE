<?xml version="1.0" ?>
<%@page contentType="text/xml; charset=iso-8859-1" language="java" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<securityCode>
  <code valid="<%= (oSubscription.m_sSecurityCode.equals(request.getParameter("code")) ? 1 : 0) %>"/>
</securityCode>
