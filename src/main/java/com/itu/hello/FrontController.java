package com.itu.hello;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.itu.methode.Scanne;
import com.itu.classe.ModelView;
import com.itu.methode.Route;

@WebServlet("/app/*")
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
        String path = req.getPathInfo() != null ? req.getPathInfo() : "/";
        String fullUrl = path + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
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
                    .filter(route -> route.getUrlPattern().matcher(fullUrl).matches())
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

                    // Initialiser tous les arguments avec des valeurs par défaut
                    for (int i = 0; i < parameters.length; i++) {
                        if (HttpServletRequest.class.isAssignableFrom(parameters[i].getType())) {
                            args[i] = req;
                        } else if (HttpServletResponse.class.isAssignableFrom(parameters[i].getType())) {
                            args[i] = resp;
                        } else {
                            String argName = parameters[i].getName();
                            if (extracted.containsKey(argName)) {
                                args[i] = convertToType(extracted.get(argName), parameters[i].getType());
                            } else {
                                args[i] = getDefaultValue(parameters[i].getType());
                            }
                        }
                    }

                    Object result = method.invoke(controllerInstance, args);

                    if (result != null && result.getClass().equals(String.class)) {
                        resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().println("<h2>Route exécutée :</h2>");
                        resp.getWriter().println("<p>URL: " + matchingRoute.getUrl() + "</p>");
                        resp.getWriter().println("<p>Classe: " + controllerClass.getSimpleName() +
                                "</p>");
                        resp.getWriter().println("<p>Méthode: " + method.getName() + "</p>");
                        resp.getWriter().println("<p>Retour: " + result.toString() + "</p>");
                    }

                    else if (result != null && result.getClass().equals(ModelView.class)) {
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
                    } else
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
}
