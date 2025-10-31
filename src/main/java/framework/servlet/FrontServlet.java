package framework.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import framework.scan.ClassScanner;

public class FrontServlet extends HttpServlet {
    List<ClassScanner> classScanners = new ArrayList<>();
    String webAppPath = "";

    @Override
    public void init() throws ServletException {
            // Obtenir le chemin de l'application web
        webAppPath = getServletContext().getRealPath("/");
        classScanners = new ClassScanner().getClassesWithMethods(webAppPath);
        for (ClassScanner classScanner : classScanners) {
            System.out.println("Class: " + classScanner.getClazz().getName());
            for (Method method : classScanner.getMethods()) {
                System.out.println(" - Method: " + method.getName());
            }
        }
        System.out.println("Servlet initialisée !");
    }

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
        String url = req.getRequestURL().toString();
        String relativePath = extractRelativePath(url);
        
        res.getWriter().write("URL recue : " + url + "\n");
        res.getWriter().write("Chemin relatif : " + relativePath + "\n\n");
        
        ClassScanner classScanner = new ClassScanner().getClassScannerByURL(url, webAppPath);
        if (classScanner != null) {
            res.getWriter().write("Classe trouvee : " + classScanner.getClazz().getName() + "\n");
            res.getWriter().write("Méthode trouvee : " + classScanner.getMethod().getName() + "\n");
        } else {
            res.getWriter().write("Aucune classe trouvee pour l'URL : " + url + "\n\n");
        }
    }
    
    private String extractRelativePath(String fullUrl) {
        // Extraire le chemin après le contexte de l'application
        try {
            int contextIndex = fullUrl.indexOf("/Framework-test/");
            if (contextIndex != -1) {
                return fullUrl.substring(contextIndex + "/Framework-test".length());
            }
            return fullUrl;
        } catch (Exception e) {
            return fullUrl;
        }
    }
}
