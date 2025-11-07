package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import framework.scan.ClassScanner;
import java.util.HashMap;

public class FrontServlet extends HttpServlet {
    List<ClassScanner> classScanners = new ArrayList<>();
    String webAppPath = "";
    HashMap<String, ClassScanner> urlToClassScannerMap = new HashMap<>();

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
        
        // Stocker urlToClassScannerMap dans le contexte servlet
        getServletContext().setAttribute("urlToClassScannerMap", urlToClassScannerMap);
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
            throws IOException, ServletException {
        String url = req.getRequestURL().toString();
        PrintWriter writer = res.getWriter();
        // writer.write("URL recue : " + url + "\n");

        ClassScanner classScanner = new ClassScanner().getClassScannerByURL(url, webAppPath);
        if (classScanner != null) {
            // writer.write("Classe trouvee : " + classScanner.getClazz().getName() + "\n");
            // writer.write("Methode trouvee : " + classScanner.getMethod().getName() + "\n\n");

            try {
                // Créer une instance de la classe
                Object instance = classScanner.getClazz().getDeclaredConstructor().newInstance();
                
                Method method = classScanner.getMethod();
                Object result = method.invoke(instance);
                
                if (method.getReturnType().equals(String.class)) {
                    writer.write((String) result);
                } else if (method.getReturnType().equals(Class.forName("framework.scan.ModelView"))) {
                    framework.scan.ModelView modelView = (framework.scan.ModelView) result;
                    String view = modelView.getView();
                    req.getRequestDispatcher(view).forward(req, res);
                }
                else {
                    writer.write("Retour non caracteriel : " + result + "\n");
                }
            } catch (Exception e) {
                res.getWriter().write("Erreur lors de l'execution de la methode : " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        } else {
            res.getWriter().write("Aucune classe trouvee pour l'URL : " + url + "\n\n");
        }
        
        urlToClassScannerMap.put(url, classScanner);

    }
}
