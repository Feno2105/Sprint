package com.itu.methode;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.itu.annotation.Controller;
import com.itu.annotation.Url;

public class Scanne {
    private Set<Route> routes = new HashSet<>();

    public Set<Route> scanPackage(String basePackage) {
        try {
            String path = basePackage.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            
            if (resource == null) {
                throw new RuntimeException("Package " + basePackage + " not found");
            }

            File directory = new File(resource.getFile());
            scanDirectory(directory, basePackage);
            return routes;
        } catch (Exception e) {
            throw new RuntimeException("Error scanning package: " + basePackage, e);
        }
    }

    private void scanDirectory(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                processClass(className);
            }
        }
    }

    private void processClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
            
            if (controllerAnnotation != null) {
                String controllerPath = controllerAnnotation.value();
                
                for (Method method : clazz.getDeclaredMethods()) {
                    Url urlAnnotation = method.getAnnotation(Url.class);
                    if (urlAnnotation != null) {
                        String methodPath = urlAnnotation.value();
                        if (methodPath.equals("none")) continue;
                        
                        // Construction de l'URL complète
                        String fullPath = controllerPath.isEmpty() ? 
                            methodPath : 
                            controllerPath + (methodPath.startsWith("/") ? methodPath : "/" + methodPath);
                        
                        // Vérification des doublons
                        Route newRoute = new Route(fullPath, method, clazz);
                        if (routes.contains(newRoute)) {
                            throw new RuntimeException("Duplicate route found: " + fullPath);
                        }
                        
                        routes.add(newRoute);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error processing class: " + className, e);
        }
    }

    public Set<Route> getRoutes() {
        return routes;
    }    
}

