<div class="container">

    <h1>User Login</h1>

    <form action="<%= response.encodeURL("j_security_check")%>" method="POST" name="loginForm" id="loginForm" style="padding: 0;">

        <div id="container" style="border:none;">

            <div id="canvas">

                <fieldset class="ui-widget ui-widget-content ui-corner-all">

                    <legend style="display: none;">User Login</legend>

                    <div>
                        <label for="j_username" style="width: 96px;">User Name:</label>
                        <input type="text" class="ui-corner-all" id="j_username" name="j_username" style="width:250px;">
                    </div>

                    <div>
                        <label for="j_password" style="width: 96px;">Password:</label>
                        <input type="password" autocomplete="off" class="ui-corner-all" id="j_password" name="j_password"
                               style="width:250px;">
                    </div>

                    <div class="login-section">
                        <button type="submit" class="btn-signin btn-dark" id="login" value="Sign In">
                            <!-- 							<i class="icon-signin"></i>  -->
                            <img src="/image/icons/light/fa-signin-shaded.png" alt="Login Icon"
                                 style="margin-bottom: -1px"/>
                            Login
                        </button>
                        <button type="button" class="btn-signin btn-light" id="register" value="Register">
                            <!-- 							<i class="icon-user"></i>  -->
                            <img src="/image/icons/dark/fa-user-shaded.png" alt="Login Icon"
                                 style="margin-bottom: -2px"/>
                            Register
                        </button>
                    </div>

                    <div style="width:auto; float:left;">
                      <a href="<%= response.encodeURL("/userAccountRetrieval.jsp") %>" class="cant-access-account">Can't access your account?</a>
                    </div>

                </fieldset>

            </div>

        </div>

        <p tabindex="0">
            Please, enter your user id and password.
            <br/>
            You may also register for a new account by clicking the 'Register' button.
        </p>

    </form>

    <div class="clearfix"></div>

</div> <!-- container -->
