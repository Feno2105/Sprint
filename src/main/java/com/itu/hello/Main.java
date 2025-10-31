package com.itu.hello;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.itu.annotation.Controller;
import com.itu.annotation.Url;
import com.itu.methode.Scanne;

public class Main {

    public static void main(String[] args) {
        // Scanner les classes avec l'annotation @Controller
        Map<String,String> data = Scanne.isOurs("/test_a");
        System.out.println("Methode et controller relier => "+ data.get("controller") + " " + data.get("methode") );        
    } 
    

   
}