package framework.scan;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import framework.annotation.Url;
import framework.annotation.Controller;

public class ClassScanner {
    private Class<?> clazz;
    private List<Method> methods;
    private Method method;

    public ClassScanner() {
    }

    public ClassScanner(Class<?> clazz, List<Method> methods) {
        this.clazz = clazz;
        this.methods = methods;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public Method getMethod() {
        return method;
    }

    public Method setMethod(Method method) {
        this.method = method;
        return method;
    }

    public List<ClassScanner> getClassesWithMethods(String webAppPath) {
        // Implémentation pour retourner les classes avec leurs méthodes
        List<ClassScanner> classesWithMethods = new ArrayList<>();

        // Scanner les classes compilées dans WEB-INF/classes
        List<String> classes = getClassesFromCompiledClasses(webAppPath);

        for (String className : classes) {
            try {
                Class<?> clazz = Class.forName(className);
                List<Method> methods = Arrays.asList(clazz.getDeclaredMethods());
                classesWithMethods.add(new ClassScanner(clazz, methods));
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + className);
                // e.printStackTrace();
            }
        }
        return classesWithMethods;
    }

    public static List<String> getClassesFromCompiledClasses(String webAppPath) {
        List<String> classes = new ArrayList<>();

        // Le dossier WEB-INF/classes dans l'application web
        File baseDir = new File(webAppPath, "WEB-INF/classes");

        if (!baseDir.exists()) {
            System.err.println("Dossier WEB-INF/classes introuvable dans " + webAppPath);
            return classes;
        }

        listerClassesCompilees(baseDir, "", classes);
        return classes;
    }

    private static void listerClassesCompilees(File dossier, String packageName, List<String> classes) {
        File[] fichiers = dossier.listFiles();
        if (fichiers == null)
            return;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                listerClassesCompilees(fichier,
                        packageName + (packageName.isEmpty() ? "" : ".") + fichier.getName(),
                        classes);
            } else if (fichier.getName().endsWith(".class")) {
                String className = packageName + (packageName.isEmpty() ? "" : ".") + fichier.getName()
                        .replace(".class", "");
                classes.add(className);
            }
        }
    }

    public boolean URLScanner(String relativePath,String methodURL){
        String[] urlParts = relativePath.split("/");
        String[] methodParts = methodURL.split("/");
        System.out.println("Comparing parts: " + Arrays.toString(urlParts) + " with " + Arrays.toString(methodParts));
        if (urlParts.length != methodParts.length) {
            return false;
        }

        for (int i = 0; i < urlParts.length; i++) {
            if (methodParts[i].startsWith("{") && methodParts[i].endsWith("}")) {
                continue;
            }
            if (!urlParts[i].equals(methodParts[i])) {
                return false;
            }
        }
        return true;
    }

    public ClassScanner getClassScannerByURL(String fullUrl, String webAppPath) {
        // Extraire le chemin relatif de l'URL
        String relativePath = extractRelativePath(fullUrl);

        List<ClassScanner> classScanners = this.getClassesWithMethods(webAppPath);
        for (ClassScanner classScanner : classScanners) {
            Class<?> clazz = classScanner.getClazz();
            if (clazz.isAnnotationPresent(Controller.class)) {
                for (Method method : classScanner.getMethods()) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        System.out.println("test.");
                        System.out.println(URLScanner(relativePath, urlAnnotation.value()));
                        if(URLScanner(relativePath, urlAnnotation.value())==true){ 
                            classScanner.setMethod(method);
                            return classScanner;
                        }
                        // System.out.println("ok.");
                        // System.out.println("Comparing: " + relativePath + " with " + urlAnnotation.value());
                        // if (relativePath.equals(urlAnnotation.value())) {
                        //     classScanner.setMethod(method);
                        //     return classScanner;
                        // }
                    }
                }
            }
        }
        return null;
    }

    private String extractRelativePath(String fullUrl) {
        // Extraire le chemin après le contexte de l'application
        // Exemple: http://localhost:8080/Framework-test/test1/method -> /test1/method
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
