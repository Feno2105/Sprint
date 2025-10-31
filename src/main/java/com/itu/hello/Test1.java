package com.itu.hello;

import com.itu.annotation.Controller;
import com.itu.annotation.Url;

@Controller
public class Test1 {
    @Url(value = "/test_a")
    public void test_a(){
    }
}


