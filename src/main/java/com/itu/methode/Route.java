package com.itu.methode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class Route {
    private String url;           
    private Method method;        
    private Class<?> controller;  
    private Pattern urlPattern;
    private List<String> urlParams;

    public Route(String url, Method method, Class<?> controller) {
        this.url = url;
        this.method = method;
        this.controller = controller;
        this.prepareRegex();
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getController() {
        return controller;
    }

    public List<String> getUrlParams() {
        return urlParams;
    }

    public void setUrlParams(List<String> urlParams) {
        this.urlParams = urlParams;
    }
        public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

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

    public void prepareRegex(){
        String regex = this.url;
        List<String> params = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^/}]+)\\}").matcher(this.url);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            params.add(paramName);
        }
        for (String param : params) {
            regex = regex.replace("{" + param + "}", "([^/]+)");
        }
        this.urlPattern = java.util.regex.Pattern.compile("^" + regex + "$");
        this.urlParams = params;
    }

}