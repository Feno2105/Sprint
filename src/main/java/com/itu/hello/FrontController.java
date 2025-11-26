package com.itu.hello;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.itu.methode.Scanne;
import com.itu.classe.ModelView;
import com.itu.methode.Route;

@WebServlet("/servlet/*")
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
        final String path = req.getPathInfo() != null ? req.getPathInfo() : "/";

        // Récupérer les routes du ServletContext
        @SuppressWarnings("unchecked")
        Set<Route> routes = (Set<Route>) getServletContext().getAttribute(ROUTES_ATTRIBUTE);
        if (path.equals("/")){
            req.setAttribute("routes", routes);
            req.getRequestDispatcher("/WEB-INF/index.jsp").forward(req, resp);
        }

        if (routes != null && !routes.isEmpty()) {
            // Chercher la route correspondante
            Route matchingRoute = routes.stream()
                    .filter(route -> route.getUrlPattern().matcher(path).matches())
                    .findFirst()
                    .orElse(null);

            if (matchingRoute != null) {
                // Route trouvée : instancier le controller et invoquer la méthode
                try {
                    Class<?> controllerClass = matchingRoute.getController();
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                    java.lang.reflect.Method method = matchingRoute.getMethod();

                    // Préparer les arguments : on injecte HttpServletRequest et HttpServletResponse
                    // si présents
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (HttpServletRequest.class.isAssignableFrom(paramTypes[i])) {
                            args[i] = req;
                        } else if (HttpServletResponse.class.isAssignableFrom(paramTypes[i])) {
                            args[i] = resp;
                        } else {
                            args[i] = null; // unsupported parameter for now
                        }
                    }
                    Map<String, String> extracted = new HashMap<>();
                    Matcher matcher = matchingRoute.getUrlPattern().matcher(path);
                        matcher.matches();
                        for (int i = 0; i < matchingRoute.getUrlParams().size(); i++) {
                            String rawValue = matcher.group(i + 1);
                            extracted.put(matchingRoute.getUrlParams().get(i), rawValue);
                        }
                    Object result = method.invoke(controllerInstance, args);
                   
                    if (result != null && result.getClass().equals(String.class)) {
                        //resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().println("<h2>Route exécutée :</h2>");
                        resp.getWriter().println("<p>URL: " + matchingRoute.getUrl() + "</p>");
                        resp.getWriter().println("<p>Classe: " + controllerClass.getSimpleName() + "</p>");
                        resp.getWriter().println("<p>Méthode: " + method.getName() + "</p>");
                        resp.getWriter().println("<p>valeur et  Paramètres url : " + extracted + "</p>");
                        resp.getWriter().println("<p>Retour: " + result.toString() + "</p>");
                    } 
                    else if (result != null && result.getClass().equals(ModelView.class)) {
                        ModelView mv = (ModelView) result;
                        Map<String,Object> data = mv.getData();
                        for (String key : data.keySet()) {
                            req.setAttribute(key, data.get(key));
                        }
                        String viewName = mv.getView();
                        String viewPath = ("/WEB-INF/views/" + viewName);
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } 
                    else resp.getWriter().println("<p>Le retour n'est pas une chaîne de caractères</p>");
                    return;
                    } 
                catch (Exception e) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().println("<h1>Erreur lors de l'exécution de la route</h1>");
                    e.printStackTrace(resp.getWriter());
                    return;
                }
            }
            else if(matchingRoute == null) {
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
}
