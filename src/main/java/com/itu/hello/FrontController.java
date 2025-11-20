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
                    .filter(route -> route.getUrlPattern().matcher(path).matches())
                    .findFirst()
                    .orElse(null);
            Map<String, String> extracted = new HashMap<>();
            if (matchingRoute != null) {
                Matcher matcher = matchingRoute.getUrlPattern().matcher(path);
                matcher.matches();
                
                for (int i = 0; i < matchingRoute.getUrlParams().size(); i++) {
                    String rawValue = matcher.group(i + 1);
                    extracted.put(matchingRoute.getUrlParams().get(i), rawValue);
                }
                // Route trouvée, afficher les détails
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().println("<h2>Route trouvée :</h2>");
                resp.getWriter().println("<p>URL: " + matchingRoute.getUrl() + "</p>");
                resp.getWriter().println("<p>Classe: " + matchingRoute.getController().getSimpleName() + "</p>");
                resp.getWriter().println("<p>Méthode: " + matchingRoute.getMethod().getName() + "</p>");
                resp.getWriter().println("<p>Paramètres url : " + matchingRoute.getUrlParams() + "</p>");
                resp.getWriter().println("<p>valeur et  Paramètres url : " + extracted + "</p>");
                return;
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
