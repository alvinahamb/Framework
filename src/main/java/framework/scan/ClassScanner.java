package framework.scan;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import framework.annotation.*;

public class ClassScanner {
    private Class<?> clazz;
    private List<Method> methods;
    private Method method;
    private HashMap<String, Object> parameterValues;
    private boolean mappedFunction = false;

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

    public Object[] getArgsForMethod() {
        Parameter[] params = this.method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String key;
            if (params[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = params[i].getAnnotation(RequestParam.class);
                key = requestParam.value();
            } else {
                key = params[i].getName();
            }
            Class<?> type = params[i].getType();
            Object value = this.parameterValues.get(key);
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

    // Bind arguments using request parameters, supporting POJO object arguments
    public Object[] getArgsForMethod(HttpServletRequest req) {
        Parameter[] params = this.method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            String key = p.isAnnotationPresent(RequestParam.class) ? p.getAnnotation(RequestParam.class).value()
                    : p.getName();
            Class<?> type = p.getType();

            // Direct support for request/response injection
            if (type.getName().equals("jakarta.servlet.http.HttpServletRequest")) {
                args[i] = req;
                continue;
            }

            if (isSimpleType(type)) {
                // Try path/query captured values first
                Object v = this.parameterValues != null ? this.parameterValues.get(key) : null;
                if (v == null) {
                    String raw = req.getParameter(key);
                    v = raw != null ? convertValue(raw, type) : getDefaultForPrimitive(type);
                }
                args[i] = v;
            } else if (Map.class.isAssignableFrom(type)) {
                args[i] = getAllPostParam(req);
            } else {
                // Treat as POJO: instantiate and populate fields from request params
                try {
                    Object pojo = type.getDeclaredConstructor().newInstance();
                    // Support paramName.field and plain field names
                    java.lang.reflect.Field[] fields = type.getDeclaredFields();
                    for (java.lang.reflect.Field f : fields) {
                        String fname = f.getName();
                        String prefixed = key + "." + fname;
                        String raw = req.getParameter(prefixed);
                        if (raw == null) {
                            raw = req.getParameter(fname);
                        }
                        if (raw == null && this.parameterValues != null) {
                            Object captured = this.parameterValues.get(prefixed);
                            if (captured == null) captured = this.parameterValues.get(fname);
                            if (captured instanceof String) raw = (String) captured;
                        }
                        if (raw != null) {
                            Object converted = convertValue(raw, f.getType());
                            boolean accessible = f.canAccess(pojo);
                            if (!accessible) f.setAccessible(true);
                            try {
                                f.set(pojo, converted);
                            } finally {
                                if (!accessible) f.setAccessible(false);
                            }
                        }
                    }
                    args[i] = pojo;
                } catch (Exception e) {
                    System.err.println("Failed to bind POJO argument for type " + type.getName() + ": " + e);
                    args[i] = null;
                }
            }
        }
        return args;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Integer.class || type == Long.class
                || type == Double.class || type == Float.class || type == Boolean.class || type == Short.class
                || type == Byte.class || type == Character.class;
    }

    private Object getDefaultForPrimitive(Class<?> type) {
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return 0;
        } else if (type == double.class || type == float.class) {
            return 0.0;
        } else if (type == boolean.class) {
            return false;
        } else if (type == char.class) {
            return '\0';
        }
        return null;
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

    public boolean URLScanner(String relativePath, String methodURL) {
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

        Parameter[] methodParameters = this.method.getParameters();
        this.parameterValues = new HashMap<>();
        for (int i = 0; i < urlParts.length; i++) {
            if (methodParts[i].startsWith("{") && methodParts[i].endsWith("}")) {
                String paramName = methodParts[i].substring(1, methodParts[i].length() - 1);
                String paramValue = urlParts[i];
                // Trouver le type du paramètre dans la méthode
                for (Parameter methodParam : methodParameters) {
                    if (methodParam.getName().equals(paramName)) {
                        Class<?> type = methodParam.getType();
                        Object convertedValue = convertValue(paramValue, type);
                        this.parameterValues.put(paramName, convertedValue);
                        break;
                    }
                }
                continue;
            }
            if (!urlParts[i].equals(methodParts[i])) {
                return false;
            }
        }
        // System.out.println("URL matched without query params.: " + urlQ);
        if (urlQ) {
            String queryString = urlPartsQ[1];
            String[] queryParams = queryString.split("&");
            Parameter[] methodParams = this.method.getParameters();
            for (String param : queryParams) {
                // System.out.println("Checking query param: " + param);
                String[] keyValue = param.split("=");
                String value = keyValue[1];
                String key = keyValue[0];
                // System.out.println("Key: " + keyValue[0] + ", Value: " + keyValue[1]);
                for (Parameter methodParam : methodParams) {
                    // System.out.println("Method param: " + methodParam.getName());
                    if (methodParam.getName().equals(keyValue[0])) {
                        // System.out.println("Matched query param: " + keyValue[0]);
                        Class<?> type = methodParam.getType();
                        Object convertedValue = convertValue(value, type);
                        this.parameterValues.put(methodParam.getName(), convertedValue);
                    } else if (methodParam.isAnnotationPresent(RequestParam.class)) {
                        RequestParam requestParam = methodParam.getAnnotation(RequestParam.class);
                        if (requestParam.value().equals(keyValue[0])) {
                            Class<?> type = methodParam.getType();
                            Object convertedValue = convertValue(value, type);
                            this.parameterValues.put(requestParam.value(), convertedValue);
                        }
                    }
                }
            }
        }
        return true;
    }

    public Map<String, Object> getAllPostParam(HttpServletRequest req) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            List<Object> convertedValues = new ArrayList<>();
            for (String value : paramValues) {
                Object converted = null;
                try {
                    converted = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        converted = Double.parseDouble(value);
                    } catch (NumberFormatException ee) {
                        try {
                            converted = Long.parseLong(value);
                        } catch (NumberFormatException eee) {
                            try {
                                converted = Float.parseFloat(value);
                            } catch (NumberFormatException eeee) {
                                try {
                                    converted = LocalDate.parse(value);
                                } catch (Exception eeeee) {
                                    if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
                                        converted = true;
                                    } else if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
                                        converted = false;
                                    } else {
                                        converted = value;
                                    }
                                }
                            }
                        }
                    }
                }
                convertedValues.add(converted);
            }
            if (convertedValues.size() == 1) {
                result.put(paramName, convertedValues.get(0));
            } else {
                boolean allStrings = true;
                for (Object v : convertedValues) {
                    if (!(v instanceof String)) {
                        allStrings = false;
                        break;
                    }
                }
                if (allStrings) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < convertedValues.size(); i++) {
                        if (i > 0)
                            sb.append(", ");
                        sb.append(convertedValues.get(i));
                    }
                    result.put(paramName, sb.toString());
                } else {
                    result.put(paramName, convertedValues);
                }
            }
        }
        // for (Map.Entry<String, Object> entry : result.entrySet()) {
        // String paramName = entry.getKey();
        // Object paramValues = entry.getValue();
        // System.out.println("Paramètre : " + paramName);
        // if (paramValues instanceof List) {
        // for (Object value : (List<?>) paramValues) {
        // System.out.println(" - Valeur : " + value);
        // }
        // } else {
        // System.out.println(" - Valeur : " + paramValues);
        // }
        // }
        return result;
    }

    private Object convertValue(String value, Class<?> targetType) {
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            } else if (targetType == char.class || targetType == Character.class) {
                return value.charAt(0);
            }
            return value;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erreur de conversion pour " + value + " vers " +
                    targetType.getName());
            // Retourner la valeur par défaut en cas d'erreur
            if (targetType.isPrimitive()) {
                return 0;
            }
            return null;
        }
    }

    public ClassScanner getClassScannerByURL(String fullUrl, String webAppPath, String httpMethod) {
        // Extraire le chemin relatif de l'URL
        String relativePath = extractRelativePath(fullUrl);
        this.mappedFunction = false;
        List<ClassScanner> classScanners = this.getClassesWithMethods(webAppPath);
        for (ClassScanner classScanner : classScanners) {
            Class<?> clazz = classScanner.getClazz();
            if (clazz.isAnnotationPresent(Controller.class)) {
                for (Method method : classScanner.getMethods()) {
                    this.method = method;
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String annotationMethod = urlAnnotation.method();
                        if (!annotationMethod.equalsIgnoreCase(httpMethod)) {
                            continue;
                        }
                        if (method.getAnnotation(Url.class).method().equalsIgnoreCase("POST") == true) {
                            System.out.println("POST method detected.");
                            if (relativePath.equals(urlAnnotation.value())) {
                                System.out.println("hita");
                                Object[] args = classScanner.getArgsForMethod();
                                // if (args.length == 1 && args[1].equals(HashMap<String, Object>)) {
                                //     mappedFunction = true;
                                // }
                                classScanner.setMethod(this.method);
                                return classScanner;
                            }
                        }
                        // System.out.println(method.getAnnotation(Url.class).method());
                        // System.out.println("test.");
                        // System.out.println(URLScanner(relativePath, urlAnnotation.value()));
                        if (URLScanner(relativePath, urlAnnotation.value()) == true) {
                            classScanner.setMethod(this.method);
                            classScanner.setParameterValues(new HashMap<>(this.parameterValues));
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
