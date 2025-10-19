package com.itu.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)  // ← Seulement METHOD
public @interface Url {
    String value();
}
