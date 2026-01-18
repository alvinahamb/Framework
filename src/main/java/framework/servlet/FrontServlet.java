package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import framework.annotation.Url;
import framework.annotation.Json;
import framework.annotation.Session;
import framework.scan.ClassScanner;
import framework.scan.*;
import java.util.HashMap;
import com.google.gson.Gson;

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
            // for (Method method : classScanner.getMethods()) {
            // System.out.println(" - Method: " + method.getName());
            // }
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
        // Ajout des valeurs apres ? dans l'url ex:
        // /test1/method3/etudiant?name=abc&id=5
        String url = req.getRequestURI();
        if (req.getQueryString() != null) {
            url += "?" + req.getQueryString();
        }
        String httpMethod = req.getMethod();
        PrintWriter writer = res.getWriter();
        // // Récupérer tous les paramètres dans une Map
        // Map<String, String[]> parameterMap = req.getParameterMap();
        // // Afficher ou traiter tous les paramètres (exemple de généralisation)
        // for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
        // String paramName = entry.getKey();
        // String[] paramValues = entry.getValue();
        // System.out.println("Paramètre : " + paramName);
        // for (String value : paramValues) {
        // System.out.println(" - Valeur : " + value);
        // }
        // }
        // writer.write("URL recue : " + url + "\n");

        ClassScanner classScanner = new ClassScanner().getClassScannerByURL(url, webAppPath, httpMethod);
        if (classScanner != null) {
            // writer.write("Classe trouvee : " + classScanner.getClazz().getName() + "\n");
            // writer.write("Methode trouvee : " + classScanner.getMethod().getName() +
            // "\n\n");
            Method method = null;
            try {
                // Créer une instance de la classe
                Object instance = classScanner.getClazz().getDeclaredConstructor().newInstance();
                method = classScanner.getMethod();
                Protection.checkRole(method, res, req);
                SessionScanner.handleSession(method, req, res);
                Object[] args = classScanner.getArgsForMethod(req);
                Object result = null;
                if (method.getAnnotation(Url.class).method().equalsIgnoreCase("POST") == true) {
                    // Bind POST using request-aware binder (supports POJO)
                    result = method.invoke(instance, classScanner.getArgsForMethod(req));
                } else {
                    result = method.invoke(instance, args);
                }
                // classScanner.test(req);
                if (method.isAnnotationPresent(Json.class)) {
                    res.setContentType("application/json");
                    Gson gson = new Gson();
                    HashMap<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("code", 200);
                    response.put("data", result);
                    writer.write(gson.toJson(response));
                } else if (method.getReturnType().equals(String.class)) {
                    writer.write((String) result);
                } else if (method.getReturnType().equals(Class.forName("framework.scan.ModelView"))) {
                    framework.scan.ModelView modelView = (framework.scan.ModelView) result;
                    String view = modelView.getView();

                    // Set all data from ModelView into request attributes
                    if (modelView.getData() != null) {
                        for (String key : modelView.getData().keySet()) {
                            req.setAttribute(key, modelView.getData().get(key));
                        }
                    }

                    req.getRequestDispatcher(view).forward(req, res);
                } else {
                    writer.write("Retour non caracteriel : " + result + "\n");
                }
            } catch (Exception e) {
                if (method != null && method.isAnnotationPresent(Json.class)) {
                    res.setContentType("application/json");
                    Gson gson = new Gson();
                    HashMap<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("code", 400);
                    response.put("data", null);
                    writer.write(gson.toJson(response));
                } else {
                    res.getWriter().write("Erreur lors de l'execution de la methode : " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }
        } else {
            res.getWriter().write("Aucune classe trouvee pour l'URL : " + url + "\n\n");
        }

        urlToClassScannerMap.put(url, classScanner);

    }
}
