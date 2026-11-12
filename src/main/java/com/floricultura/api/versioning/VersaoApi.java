package com.floricultura.api.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marca a versao de um endpoint que existe em mais de uma versao.
// serve pro Swagger separar os grupos "v1" e "v2" (o roteamento em si e feito pelo
// headers = "X-API-Version=2" no @GetMapping).
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VersaoApi {
    int value();
}
