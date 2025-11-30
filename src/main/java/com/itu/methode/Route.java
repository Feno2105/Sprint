package com.itu.methode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {
    private String url;
    private Method method;
    private Class<?> controller;
    private Pattern urlPattern;

    public Route(String url, Method method, Class<?> controller) {
        this.url = url;
        this.method = method;
        this.controller = controller;
        this.prepareRegex();
    }

    // Getters
    public String getUrl() { return url; }
    public Method getMethod() { return method; }
    public Class<?> getController() { return controller; }
    public Pattern getUrlPattern() { return urlPattern; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Route)) return false;
        Route other = (Route) obj;
        return this.url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    public void prepareRegex() {
        String regex = this.url;
        
        // Transformer les {param} en regex
        Matcher matcher = Pattern.compile("\\{([^/}]+)\\}").matcher(this.url);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            regex = regex.replace("{" + paramName + "}", "([^/]+)");
        }

        // Ajouter le support optionnel des paramètres query string
        regex = regex + "(?:\\?.*)?";
        
        this.urlPattern = Pattern.compile("^" + regex + "$");
    }

    public Map<String, String> extractParameters(String requestUrl) {
    Map<String, String> parameters = new HashMap<>();
    
    // Si l'URL contient des {...}, extraire les paramètres de chemin
    if (this.url.contains("{")) {
        // Extraire les paramètres de chemin {param}
        Matcher pathMatcher = this.urlPattern.matcher(requestUrl);
        if (pathMatcher.matches()) {
            // Récupérer les noms des paramètres depuis l'URL originale
            List<String> pathParamNames = new ArrayList<>();
            Matcher paramNameMatcher = Pattern.compile("\\{([^/}]+)\\}").matcher(this.url);
            while (paramNameMatcher.find()) {
                pathParamNames.add(paramNameMatcher.group(1));
            }
            
            // Assigner les valeurs aux noms
            for (int i = 0; i < pathParamNames.size() && (i + 1) <= pathMatcher.groupCount(); i++) {
                String paramName = pathParamNames.get(i);
                String paramValue = pathMatcher.group(i + 1);
                parameters.put(paramName, paramValue);
            }
        }
    }
    
    // SI l'URL contient ?, extraire les paramètres query string
    else if (requestUrl.contains("?")) {
        String queryString = requestUrl.substring(requestUrl.indexOf('?') + 1);
        String[] queryParams = queryString.split("&");
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
    }
    
    return parameters;
}
}