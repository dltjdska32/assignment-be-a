package com.assginment.be_a.infra.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record Response<T>(HttpStatusCode statusCode, String code, String message, T value) {


    public static <T> Response<T> ok(){

        return new Response<>(HttpStatus.OK,"SUCCESS", "ok", null);
    }


    public static <T> Response<T> ok(T value){

        return new Response<>(HttpStatus.OK,"SUCCESS", "ok", value);
    }


    public static <T> Response<T> ok(HttpStatusCode statusCode, T value){

        return new Response<>(statusCode,"SUCCESS", "ok", value);
    }


    public static <T> Response<T> error(HttpStatusCode statusCode, String code, String message){
        return new Response<>(statusCode, code, message, null);
    }
}