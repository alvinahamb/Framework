package src;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        affichage(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        affichage(req, res);
    }

    private void affichage(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String path = req.getRequestURI();
        res.getWriter().println("URL capturée par FrontServlet : " + path);
    }
};