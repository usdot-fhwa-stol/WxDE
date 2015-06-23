/************************************************************************
 * Source filename: AccessControl.java
 * <p/>
 * Creation date: Sep 6, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.security;

import org.apache.catalina.realm.GenericPrincipal;

import javax.servlet.http.HttpServletRequest;

public class AccessControl {

    public static boolean isSuperUser(HttpServletRequest request) {
        return hasRole(request, "wde_admin");
    }

    public static boolean hasRole(HttpServletRequest request, String role) {
        boolean flag = false;

        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
        if (principal != null) {
            String[] roles = principal.getRoles();

            for (String arole : roles)
                if (arole.equals(role)) {
                    flag = true;
                    break;
                }
        }
        return flag;
    }
}
