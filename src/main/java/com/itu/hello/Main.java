package com.itu.hello;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.itu.annotation.Controller;



public class Main {
    
    public static void main(String[] args) {
        // Scanner les classes avec l'annotation @Controller
        List<String> controllers = trouverControllers();
        
        System.out.println("Classes annotées avec @Controller :");
        for (String controller : controllers) {
            System.out.println("- " + controller);
        }
    }
    
    public static List<String> trouverControllers() {
        List<String> nomsControllers = new ArrayList<>();
        
        try {
            // Obtenir le package de base (adaptez selon votre structure)
            String packageName = "com.itu"; // ou "com.votrepackage"
            String chemin = packageName.replace('.', '/');
            
            // Obtenir l'URL du répertoire depuis le classpath
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(chemin);
            
            if (resource != null) {
                File repertoire = new File(resource.getFile());
                scannerRepertoire(repertoire, packageName, nomsControllers);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return nomsControllers;
    }
    
    private static void scannerRepertoire(File repertoire, String packageName, List<String> controllers) {
        if (!repertoire.exists() || !repertoire.isDirectory()) {
            return;
        }
        
        File[] fichiers = repertoire.listFiles();
        if (fichiers == null) return;
        
        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                // Scanner récursivement les sous-répertoires
                String nouveauPackage = packageName.isEmpty() ? 
                    fichier.getName() : packageName + "." + fichier.getName();
                scannerRepertoire(fichier, nouveauPackage, controllers);
                
            } else if (fichier.getName().endsWith(".class")) {
                // Vérifier si le fichier .class a l'annotation @Controller
                verifierAnnotationController(fichier, packageName, controllers);
            }
        }
    }
    
    private static void verifierAnnotationController(File fichier, String packageName, List<String> controllers) {
        try {
            // Extraire le nom de la classe du nom de fichier
            String nomFichier = fichier.getName();
            String nomClasse = nomFichier.substring(0, nomFichier.length() - 6); // enlever ".class"
            
            // Construire le nom complet de la classe
            String nomCompletClasse = packageName.isEmpty() ? 
                nomClasse : packageName + "." + nomClasse;
            
            // Charger la classe
            Class<?> classe = Class.forName(nomCompletClasse);
            
            // Vérifier si la classe a l'annotation @Controller
            if (classe.isAnnotationPresent(Controller.class)) {
                controllers.add(nomCompletClasse);
                
                // Optionnel : afficher les détails de l'annotation
                Controller annotation = classe.getAnnotation(Controller.class);
                String valeur = annotation.value();
                if (!valeur.isEmpty()) {
                    System.out.println("Valeur de l'annotation : " + valeur);
                }
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Classe non trouvée : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la classe : " + e.getMessage());
        }
    }
}