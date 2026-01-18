package framework.scan;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import jakarta.servlet.http.*;
import framework.annotation.Session;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Protection {
    public static boolean checkRole(Method method, HttpServletResponse res, HttpServletRequest req) throws IOException {
        if (method.isAnnotationPresent(framework.annotation.Authorized.class)) {
            return true; // Access granted for authorized methods
        }
        if (method.isAnnotationPresent(framework.annotation.Role.class)) {
            framework.annotation.Role roleAnnot = method.getAnnotation(framework.annotation.Role.class);
            String[] allowedRoles = roleAnnot.value();
            HttpSession session = req.getSession(false);
            PrintWriter out = res.getWriter();

            if (session == null || session.getAttribute("userRole") == null) {
                out.println("Access Denied: No active session or user role found.");
                return false;
            }

            String userRole = (String) session.getAttribute("userRole");
            for (String role : allowedRoles) {
                if (role.equals(userRole)) {
                    return true; // Access granted
                }
            }
            out.println("Access Denied: User role '" + userRole + "' is not authorized.");
            return false; // Access denied
        }
        return true; // No role restriction, access granted
    }
}