package framework.scan;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import jakarta.servlet.http.*;
import framework.annotation.Session;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SessionScanner {
    private static String key;
    private static Object value;
    
    public String getKey() {
        return key;
    }

    public static Object getValue() {
        return value;
    }

    public static void handleSession(Method method, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (method.isAnnotationPresent(Session.class)) {
            Session sessionAnnot = method.getAnnotation(Session.class);
            String action = sessionAnnot.action();
            key = sessionAnnot.key();
            value = sessionAnnot.value();
            PrintWriter out = res.getWriter();
            
            if ("get".equalsIgnoreCase(action)) {
                Object sessionValue = req.getSession().getAttribute(key);
                value = sessionValue;
                out.println("Session Value for key '" + key + "': " + sessionValue);

            } else if ("set".equalsIgnoreCase(action)) {
                req.getSession().setAttribute(key, value);
                out.println("Session Value set for key '" + key + "' to: " + value);
            } else if ("remove".equalsIgnoreCase(action)) {
                req.getSession().removeAttribute(key);
                out.println("Session Value removed for key '" + key + "'");
            }
        }
    }
}
