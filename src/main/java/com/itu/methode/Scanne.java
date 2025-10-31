package com.itu.methode;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itu.annotation.Controller;
import com.itu.annotation.Url;

public class Scanne {

    public static Map<String,String> isOurs(String lien){
        Map<String, Object> all = trouverControllers_urls();
        
        System.out.println("Classes annotées avec @Controller :");
        // List<String> controllers = (List<String>) all.get("controllers");
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> controllerMethods = (Map<String, List<Map<String, String>>>) all.get("controllerMethods");
        Map<String, String> result  =  new HashMap<>();
        
        if (!controllerMethods.isEmpty()) {
            System.out.println("\n Urls et méthodes trouvés par contrôleur:");
           
        for (String controllerName : controllerMethods.keySet()) {
        System.out.println("Contrôleur: " + controllerName);
        List<Map<String, String>> methods = controllerMethods.get(controllerName);
        for (Map<String, String> urlMethod : methods) {
            if (urlMethod.get("url").equals(lien)) {
                System.out.println("url => " + urlMethod.get("url") +" lien "+ lien);
                result.put("controller", controllerName);
                result.put("methode", urlMethod.get("method"));
            }
            System.out.println("  URL: " + urlMethod.get("url") + " -> Méthode: " + urlMethod.get("method"));
            }
        }
    } 
    else {
    System.out.println("Aucun lien url trouvé");
    }
        return result;
        
    } 
    public static Map<String, Object> trouverControllers_urls() {
        List<String> nomsControllers = new ArrayList<>();
        Map<String, List<Map<String, String>>> controllerMethods = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        try {
            String packageName = "com.itu";
            String chemin = packageName.replace('.', '/');

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(chemin);

            if (resource != null) {
                System.out.println("scanne du repertoire ");
                File repertoire = new File(resource.getFile());
                scannerRepertoire(repertoire, packageName, nomsControllers, controllerMethods);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.put("controllers", nomsControllers);
        result.put("controllerMethods", controllerMethods);
        return result;
    }

    private static void scannerRepertoire(File repertoire, String packageName,
            List<String> controllers,
            Map<String, List<Map<String, String>>> controllerMethods) {
        if (!repertoire.exists() || !repertoire.isDirectory()) {
            return;
        }

        System.out.println(packageName);
        File[] fichiers = repertoire.listFiles();
        if (fichiers == null)
            return;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                String nouveauPackage = packageName.isEmpty() ? fichier.getName()
                        : packageName + "." + fichier.getName();
                scannerRepertoire(fichier, nouveauPackage, controllers, controllerMethods);

            } else if (fichier.getName().endsWith(".class")) {
                String nomControleur = verifierAnnotationController(fichier, packageName, controllers);
                if (nomControleur != null) {
                    List<Map<String, String>> methodesAvecUrl = verifierAnnotationUrl(fichier, packageName);
                    if (!methodesAvecUrl.isEmpty()) {
                        controllerMethods.put(nomControleur, methodesAvecUrl);
                    }
                }
            }
        }
    }

    private static String verifierAnnotationController(File fichier, String packageName, List<String> controllers) {
        try {
            String nomFichier = fichier.getName();
            String nomClasse = nomFichier.substring(0, nomFichier.length() - 6);
            String nomCompletClasse = packageName.isEmpty() ? nomClasse : packageName + "." + nomClasse;

            Class<?> classe = Class.forName(nomCompletClasse);

            if (classe.isAnnotationPresent(Controller.class)) {
                controllers.add(nomCompletClasse);

                Controller annotation = classe.getAnnotation(Controller.class);
                String valeur = annotation.value();
                if (!valeur.isEmpty()) {
                    System.out.println("Valeur de l'annotation : " + valeur);
                }
                return nomCompletClasse;
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Classe non trouvée : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la classe cccc : " + e.getMessage());
        }
        return null;
    }

    private static List<Map<String, String>> verifierAnnotationUrl(File fichier, String packageName) {
        List<Map<String, String>> methodesAvecUrl = new ArrayList<>();

        try {
            String nomFichier = fichier.getName();
            String nomClasse = nomFichier.substring(0, nomFichier.length() - 6);
            System.out.println("nom de classe: " + nomClasse);

            String nomCompletClasse = packageName.isEmpty() ? nomClasse : packageName + "." + nomClasse;

            Class<?> classe = Class.forName(nomCompletClasse);
            Method[] methodes = classe.getDeclaredMethods();

            for (Method method : methodes) {
                if (method.isAnnotationPresent(Url.class)) {
                    Map<String, String> urlInfo = new HashMap<>();
                    Url annotation = method.getAnnotation(Url.class);
                    String valeur = annotation.value();

                    urlInfo.put("url", valeur);
                    urlInfo.put("method", method.getName());
                    methodesAvecUrl.add(urlInfo);

                    System.out.println("Valeur de l'annotation : " + valeur + " la methode est " + method.getName());
                }
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Classe non trouvée : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la classe xxxx : " + e.getMessage());
        }

        return methodesAvecUrl;
    }
}
