package com.itu.methode;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/*")
public class FrontController extends HttpServlet {
    RequestDispatcher defaultDispatcher;
    private Map<String,Object> Route = new HashMap<>();

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        Route =  Scanne.trouverControllers_urls();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo();
       Map<String,String> result =  Scanne.isOurs(path);
        if (!result.isEmpty()) {
            defaultServe(req, resp, result);
        } else {
             if (path == null) path = "/";
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println("<h1> :: Tu as demandé : " + path + "</h1>");
        }
    }
    private void defaultServe(HttpServletRequest req, HttpServletResponse res,Map<String,String> result) throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
            res.getWriter().println("<h1>BIS :: Controller : " + result.get("controller") + "</h1>");
            res.getWriter().println("<h1>BIS :: methode : " + result.get("methode") + "</h1>");

    }
}
