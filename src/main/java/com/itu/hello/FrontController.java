package com.itu.hello;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import com.itu.methode.Scanne;
import com.itu.methode.Route;

@WebServlet("/*")
public class FrontController extends HttpServlet {
    private static final String ROUTES_ATTRIBUTE = "routes";
    
    private RequestDispatcher defaultDispatcher;
    
    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        
        // Scanner les routes
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
        
        if (routes != null) {
            // Chercher la route correspondante
            Route matchingRoute = routes.stream()
                .filter(route -> route.getUrl().equals(path))
                .findFirst()
                .orElse(null);
            
            if (matchingRoute != null) {
                // Route trouvée : instancier le controller et invoquer la méthode
                try {
                    Class<?> controllerClass = matchingRoute.getController();
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                    java.lang.reflect.Method method = matchingRoute.getMethod();

                    // Préparer les arguments : on injecte HttpServletRequest et HttpServletResponse si présents
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    if (paramTypes.length > 0) {
                        // for (int i = 0; i < paramTypes.length; i++) {
                        //     if (paramTypes[i].isAssignableFrom(HttpServletRequest.class)) {
                        //         args[i] = req;
                        //     } else if (paramTypes[i].isAssignableFrom(HttpServletResponse.class)) {
                        //         args[i] = resp;
                        //     } else {
                        //         // Paramètre non supporté : on met null (ou on pourrait lever une erreur)
                        //         args[i] = null;
                        //     }
                        // }
                    }

                    Object result = method.invoke(controllerInstance, args);
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.getWriter().println("<h2>Route exécutée :</h2>");
                    resp.getWriter().println("<p>URL: " + matchingRoute.getUrl() + "</p>");
                    resp.getWriter().println("<p>Classe: " + controllerClass.getSimpleName() + "</p>");
                    resp.getWriter().println("<p>Méthode: " + method.getName() + "</p>");
                    if (result != null &&result.getClass().equals(String.class)) {
                        resp.getWriter().println("<p>Retour: " + result.toString() + "</p>");
                    }
                    else resp.getWriter().println("<p>Le retour n'est pas une chaîne de caractères</p>");
                    return;
                } catch (Exception e) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().println("<h1>Erreur lors de l'exécution de la route</h1>");
                    e.printStackTrace(resp.getWriter());
                    return;
                }
            }
        }
        
        // Vérifier si c'est une ressource statique
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            defaultServe(req, resp);
        } else {
            // Route non trouvée
            resp.setContentType("text/html;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().println("<h1>Aucun url de ce type n'a été trouvé 404 not found by the server</h1>");
        }
    }
    
    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
