<?xml version="1.0" ?>
<%@page contentType="text/xml; charset=UTF-8" language="java" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<setPassword>
  <results passwordMatches="<%= oSubscription.checkSecurity() %>"/>
</setPassword>
