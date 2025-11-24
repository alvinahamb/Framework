package framework.scan;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import framework.annotation.Url;
import framework.annotation.*;


public class ClassScanner {
    private Class<?> clazz;
    private List<Method> methods;
    private Method method;
    private HashMap<String, Object> parameterValues;

    public ClassScanner() {
    }

    public ClassScanner(Class<?> clazz, List<Method> methods) {
        this.clazz = clazz;
        this.methods = methods;
        this.parameterValues = new HashMap<>();
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

    public HashMap<String, Object> getParameterValues() {
        return parameterValues;
    }

    public void setParameterValues(HashMap<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
    }

    public Object[] getArgsForMethod(Method method) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String name = params[i].getName();
            Class<?> type = params[i].getType();
            Object value = this.parameterValues.get(name);
            if (value == null && type.isPrimitive()) {
                if (type == int.class || type == long.class || type == short.class || type == byte.class) {
                    value = 0;
                } else if (type == double.class || type == float.class) {
                    value = 0.0;
                } else if (type == boolean.class) {
                    value = false;
                } else if (type == char.class) {
                    value = '\0';
                }
            }
            args[i] = value;
        }
        return args;
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

    public boolean URLScanner(String relativePath, String methodURL, Method method) {
        boolean urlQ = false;
        // System.out.println("Relative Path: " + relativePath);
        // System.out.println(relativePath.contains("?")+" contains ?");
        String[] urlPartsQ = new String[2];
        if (relativePath.contains("?")) {
            urlPartsQ = relativePath.split("\\?");
            relativePath = urlPartsQ[0];
            urlQ = true;
        }
        String[] urlParts = relativePath.split("/");
        String[] methodParts = methodURL.split("/");
        // System.out.println("Comparing parts: " + Arrays.toString(urlParts) + " with "
        // + Arrays.toString(methodParts));
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
        this.parameterValues = new HashMap<>();
        // System.out.println("URL matched without query params.: " + urlQ);
        if (urlQ) {
            String queryString = urlPartsQ[1];
            String[] queryParams = queryString.split("&");
            Parameter[] methodParams = method.getParameters();
            for (String param : queryParams) {
                // System.out.println("Checking query param: " + param);
                String[] keyValue = param.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // System.out.println("Key: " + keyValue[0] + ", Value: " + keyValue[1]);
                for (Parameter methodParam : methodParams) {
                    System.out.println("Method param: " + methodParam.getName());
                    if (methodParam.getName().equals(keyValue[0])) {
                        System.out.println("Matched query param: " + keyValue[0]);
                        Class<?> type = methodParam.getType();
                        if (type.isPrimitive()) {
                            // Pour les types primitifs numériques: initialiser à 0
                            if (type == int.class || type == long.class || type == short.class || type == byte.class) {
                                this.parameterValues.put(key, 0);
                            }
                            // Pour float et double: initialiser à 0.0
                            else if (type == double.class || type == float.class) {
                                this.parameterValues.put(key, 0.0);
                            }
                            // Pour boolean: initialiser à false
                            else if (type == boolean.class) {
                                this.parameterValues.put(key, false);
                            }
                            // Pour char: initialiser à '\0' (caractère nul)
                            else if (type == char.class) {
                                this.parameterValues.put(key, '\0');
                            }
                        } else {
                            // Pour les objets (String, Integer, etc.): initialiser à null
                            this.parameterValues.put(key, null);
                        }
                        // Object convertedValue = convertValue(value, type);
                        // this.parameterValues.put(key, convertedValue);
                        break;
                    }
                    else if (methodParam.isAnnotationPresent(RequestParam.class)) {
                        RequestParam requestParam = methodParam.getAnnotation(RequestParam.class);
                        if (requestParam.value().equals(keyValue[0])) {
                            Class<?> type = methodParam.getType();
                            if (type.isPrimitive()) {
                                // Pour les types primitifs numériques: initialiser à 0
                                if (type == int.class || type == long.class || type == short.class || type == byte.class) {
                                    this.parameterValues.put(key, 0);
                                }
                                // Pour float et double: initialiser à 0.0
                                else if (type == double.class || type == float.class) {
                                    this.parameterValues.put(key, 0.0);
                                }
                                // Pour boolean: initialiser à false
                                else if (type == boolean.class) {
                                    this.parameterValues.put(key, false);
                                }
                                // Pour char: initialiser à '\0' (caractère nul)
                                else if (type == char.class) {
                                    this.parameterValues.put(key, '\0');
                                }
                            } else {
                                // Pour les objets (String, Integer, etc.): initialiser à null
                                this.parameterValues.put(key, null);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    // private Object convertValue(String value, Class<?> targetType) {
    // try {
    // if (targetType == String.class) {
    // return value;
    // } else if (targetType == int.class || targetType == Integer.class) {
    // return Integer.parseInt(value);
    // } else if (targetType == long.class || targetType == Long.class) {
    // return Long.parseLong(value);
    // } else if (targetType == double.class || targetType == Double.class) {
    // return Double.parseDouble(value);
    // } else if (targetType == float.class || targetType == Float.class) {
    // return Float.parseFloat(value);
    // } else if (targetType == boolean.class || targetType == Boolean.class) {
    // return Boolean.parseBoolean(value);
    // } else if (targetType == short.class || targetType == Short.class) {
    // return Short.parseShort(value);
    // } else if (targetType == byte.class || targetType == Byte.class) {
    // return Byte.parseByte(value);
    // }
    // return value;
    // } catch (NumberFormatException e) {
    // System.err.println("Erreur de conversion pour " + value + " vers " +
    // targetType.getName());
    // // Retourner la valeur par défaut en cas d'erreur
    // if (targetType.isPrimitive()) {
    // return 0;
    // }
    // return null;
    // }
    // }

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
                        System.out.println(URLScanner(relativePath, urlAnnotation.value(), method));
                        if (URLScanner(relativePath, urlAnnotation.value(), method) == true) {
                            classScanner.setMethod(method);
                            return classScanner;
                        }
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
