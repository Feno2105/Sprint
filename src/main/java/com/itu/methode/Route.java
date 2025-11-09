package com.itu.methode;

import java.lang.reflect.Method;

public class Route {
    private String url;           
    private Method method;        
    private Class<?> controller;  

    public Route(String url, Method method, Class<?> controller) {
        this.url = url;
        this.method = method;
        this.controller = controller;
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
}