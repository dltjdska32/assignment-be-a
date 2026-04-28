package com.assginment.be_a.infra.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class BaseException extends RuntimeException {


    private final HttpStatusCode statusCode;
    private final String code;
    private final String message;

    public BaseException(HttpStatusCode statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
    }
}