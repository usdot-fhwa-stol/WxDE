
<script type="text/javascript">
  <%
    String nonceParam = response.encodeURL("/");
    int index = nonceParam.indexOf("/?");
    nonceParam = index >= 0 ? nonceParam.substring(index + 2) : "";
  %>
  csrf_nonce_param = '<%= nonceParam %>';
</script>

	<script src="/script/jquery/jquery-1.9.1.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>
	<!-- script for the main menu -->
	<script src="/script/jquery/superfish.js" type="text/javascript"></script>
	<!-- script for highlighting the main-menu link the page is under -->
    <script src="/script/menuHighlight.js" type="text/javascript"></script>