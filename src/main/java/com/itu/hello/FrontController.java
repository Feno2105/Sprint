package com.itu.hello;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.itu.methode.Scanne;
import com.itu.classe.ModelView;
import com.itu.methode.Route;
import com.itu.annotation.HttpMethod;
import com.itu.annotation.Json;
import com.google.gson.Gson;

@WebServlet("/app/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class FrontController extends HttpServlet {
    private static final String ROUTES_ATTRIBUTE = "routes";

    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            Scanne scanner = new Scanne();
            Set<Route> routes = scanner.scanPackage("com.itu");

            // Stocker les routes dans le ServletContext
            getServletContext().setAttribute(ROUTES_ATTRIBUTE, routes);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des routes", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPrepare(req, resp, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPrepare(req, resp, "POST");
    }

    protected void doPrepare(HttpServletRequest req, HttpServletResponse resp, String httpMethod)
            throws IOException, ServletException {
        String path = req.getPathInfo() != null ? req.getPathInfo() : "/";
        String fullUrl = path + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        req.setAttribute("httpMethod", httpMethod);
        // Récupérer les routes du ServletContext
        @SuppressWarnings("unchecked")
        Set<Route> routes = (Set<Route>) getServletContext().getAttribute(ROUTES_ATTRIBUTE);
        if (path.equals("/")) {
            req.setAttribute("routes", routes);
            req.getRequestDispatcher("/WEB-INF/index.jsp").forward(req, resp);
        }

        if (routes != null && !routes.isEmpty()) {
            // Chercher la route correspondante
            Route matchingRoute = routes.stream()
                    .filter(route -> route.getUrlPattern().matcher(fullUrl).matches() 
                            && route.getHttpMethod().name().equals(httpMethod))
                    .findFirst()
                    .orElse(null);

            if (matchingRoute != null) {
                try {
                    // classe et methode à exécuter
                    Class<?> controllerClass = matchingRoute.getController();
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                    java.lang.reflect.Method method = matchingRoute.getMethod();
                    Parameter[] parameters = method.getParameters();
                    Object[] args = new Object[parameters.length];

                    Map<String, String> extracted = matchingRoute.extractParameters(fullUrl);
                    
                    // Gérer les fichiers uploadés si la requête est multipart
                    File uploadedFile = null;
                    if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {
                        try {
                            Part filePart = req.getPart("file");
                            if (filePart != null && filePart.getSubmittedFileName() != null && !filePart.getSubmittedFileName().isEmpty()) {
                                String fileName = filePart.getSubmittedFileName();
                                
                                // Créer un répertoire temporaire s'il n'existe pas
                                File tempDir = new File(System.getProperty("java.io.tmpdir"), "uploads");
                                if (!tempDir.exists()) {
                                    tempDir.mkdirs();
                                }
                                
                                // Créer un fichier temporaire avec un nom unique
                                uploadedFile = new File(tempDir, System.currentTimeMillis() + "_" + fileName);
                                
                                // Lire et écrire le fichier manuellement
                                try (InputStream input = filePart.getInputStream();
                                     FileOutputStream output = new FileOutputStream(uploadedFile)) {
                                    
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    long totalBytesRead = 0;
                                    
                                    while ((bytesRead = input.read(buffer)) != -1) {
                                        output.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;
                                    }
                                    
                                    output.flush();
                                }
                                
                                // Vérifier que le fichier a bien été écrit
                                if (uploadedFile.exists() && uploadedFile.length() > 0) {
                                    uploadedFile.deleteOnExit();
                                } else {
                                    if (uploadedFile.exists()) {
                                        uploadedFile.delete();
                                    }
                                    uploadedFile = null;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // Initialiser tous les arguments avec des valeurs par défaut
                    for (int i = 0; i < parameters.length; i++) {
                        if (HttpServletRequest.class.isAssignableFrom(parameters[i].getType())) {
                            args[i] = req;
                        } else if (HttpServletResponse.class.isAssignableFrom(parameters[i].getType())) {
                            args[i] = resp;
                        } else if (Map.class.isAssignableFrom(parameters[i].getType())) {
                            // Si le paramètre est un Map, le remplir avec tous les paramètres
                            Map<String, Object> paramMap = new HashMap<>();
                            
                            // Ajouter les paramètres extraits de l'URL
                            for (String key : extracted.keySet()) {
                                paramMap.put(key, extracted.get(key));
                            }
                            
                            // Ajouter tous les paramètres de la requête (formulaire, query string)
                            Map<String, String[]> requestParams = req.getParameterMap();
                            for (String key : requestParams.keySet()) {
                                String[] values = requestParams.get(key);
                                if (values.length == 1) {
                                    paramMap.put(key, values[0]);
                                } else {
                                    paramMap.put(key, values);
                                }
                            }
                            
                            args[i] = paramMap;
                        } else if (File.class.isAssignableFrom(parameters[i].getType())) {
                            // Si le paramètre est un File, utiliser le fichier uploadé
                            args[i] = uploadedFile;
                        } else if (isCustomObject(parameters[i].getType())) {
                            // Si c'est un objet custom, l'instancier et le remplir
                            args[i] = fillCustomObject(parameters[i].getType(), req);
                        } else {
                            String argName = parameters[i].getName();
                            if (extracted.containsKey(argName)) {
                                args[i] = convertToType(extracted.get(argName), parameters[i].getType());
                            } 
                            else {
                                args[i] = getDefaultValue(parameters[i].getType());
                            }
                        }
                    }

                    Object result = method.invoke(controllerInstance, args);

                    // Vérifier si la méthode est annotée avec @Json
                    
                    
                    if (result != null && result.getClass().equals(String.class)) {
                        resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().println("<h2>Route exécutée :</h2>");
                        resp.getWriter().println("<p>URL: " + matchingRoute.getUrl() + "</p>");
                        resp.getWriter().println("<p>Classe: " + controllerClass.getSimpleName() +
                                "</p>");
                        resp.getWriter().println("<p>Méthode: " + method.getName() + "</p>");
                        resp.getWriter().println("<p>Retour: " + result.toString() + "</p>");
                    }

                    else if (result != null && result.getClass().equals(ModelView.class) && !method.isAnnotationPresent(Json.class)) {
                        for (String key : extracted.keySet()) {
                            // resp.getWriter().println("<p>Param URL: " + key + " = " + extracted.get(key)
                            // + "</p>");
                            req.setAttribute(key, extracted.get(key));
                        }
                        ModelView mv = (ModelView) result;
                        Map<String, Object> data = mv.getData();
                        for (String key : data.keySet()) {
                            req.setAttribute(key, data.get(key));
                        }
                        String viewName = mv.getView();
                        String viewPath = ("/WEB-INF/views/" + viewName);
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } 
                    else if (method.isAnnotationPresent(Json.class)) {
                        resp.setContentType("application/json;charset=UTF-8");
                        Gson gson = new Gson();
                        Object jsonData = result;
                        
                        // Si le résultat est un ModelView, extraire seulement les données
                        if (result != null && result instanceof ModelView) {
                            ModelView mv = (ModelView) result;
                            jsonData = mv.getData();
                            System.out.println("DEBUG: Extraction des données du ModelView: " + jsonData);
                        }
                        
                        String json = gson.toJson(jsonData);
                        System.out.println("DEBUG: JSON généré: " + json);
                        resp.getWriter().write(json);
                        return;
                    }
                    else
                        resp.getWriter().println("<p>Le retour n'est pas une chaîne de caractères</p>");
                    return;
                } catch (Exception e) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().println("<h1>Erreur lors de l'exécution de la route</h1>");
                    e.printStackTrace(resp.getWriter());
                    return;
                }
            }

            else if (matchingRoute == null) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().println("<p>Aucune route trouvée, servir les ressources statiques</p>");
                defaultServe(req, resp);
                return;
            }
        }
    }
    

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }

    private Object convertToType(String value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }

        try {
            if (targetType.equals(String.class)) {
                return value;
            } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.valueOf(value);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.valueOf(value);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.valueOf(value);
            } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return Boolean.valueOf(value);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.valueOf(value);
            }
        } catch (NumberFormatException e) {
            return getDefaultValue(targetType);
        }

        return value; // Fallback pour les types non gérés
    }

    private Object getDefaultValue(Class<?> targetType) {
        if (targetType.isPrimitive()) {
            if (targetType == int.class)
                return 0;
            if (targetType == long.class)
                return 0L;
            if (targetType == double.class)
                return 0.0;
            if (targetType == boolean.class)
                return false;
            if (targetType == float.class)
                return 0.0f;
            if (targetType == byte.class)
                return (byte) 0;
            if (targetType == short.class)
                return (short) 0;
            if (targetType == char.class)
                return '\0';
        }
        return null;
    }

    private boolean isCustomObject(Class<?> type) {
        // Vérifier si c'est un objet custom (ni primitif, ni wrapper, ni String, ni Map, ni Request/Response)
        return !type.isPrimitive() 
            && !type.equals(String.class)
            && !type.equals(Integer.class)
            && !type.equals(Long.class)
            && !type.equals(Double.class)
            && !type.equals(Boolean.class)
            && !type.equals(Float.class)
            && !Map.class.isAssignableFrom(type)
            && !HttpServletRequest.class.isAssignableFrom(type)
            && !HttpServletResponse.class.isAssignableFrom(type);
    }

    private Object fillCustomObject(Class<?> type, HttpServletRequest req) throws Exception {
        // Créer une instance de l'objet
        Object instance = type.getDeclaredConstructor().newInstance();
        
        // Récupérer tous les paramètres de la requête
        Map<String, String[]> requestParams = req.getParameterMap();
        
        // Pour chaque paramètre, chercher un setter correspondant
        for (String paramName : requestParams.keySet()) {
            String[] values = requestParams.get(paramName);
            if (values != null && values.length > 0) {
                String value = values[0];
                
                // Construire le nom du setter (ex: nom -> setNom)
                String setterName = "set" + paramName.substring(0, 1).toUpperCase() + paramName.substring(1);
                
                try {
                    // Chercher le setter avec différents types de paramètres
                    java.lang.reflect.Method setter = findSetter(type, setterName);
                    if (setter != null) {
                        Class<?> paramType = setter.getParameterTypes()[0];
                        Object convertedValue = convertToType(value, paramType);
                        setter.invoke(instance, convertedValue);
                    }
                } catch (Exception e) {
                    // Ignorer si le setter n'existe pas ou échoue
                }
            }
        }
        
        return instance;
    }

    private java.lang.reflect.Method findSetter(Class<?> type, String setterName) {
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] tokens = contentDisposition.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "unknown";
    }
}
