package com.assginment.be_a.application.exception;

import com.assginment.be_a.infra.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class ProductException extends BaseException {

    public ProductException(HttpStatusCode statusCode, String code, String message) {
        super(statusCode, code, message);
    }


    public static ProductException unauthorized(String message) {
        return new ProductException(HttpStatus.UNAUTHORIZED, "PRODUCT_ERR_01" , message);
    }

    public static ProductException badRequest(String message) {
        return new ProductException(HttpStatus.BAD_REQUEST, "PRODUCT_ERR_02" , message);
    }

    public static ProductException apiErr(String message) {
        return new ProductException(HttpStatus.BAD_REQUEST, "PRODUCT_ERR_03" , message);
    }

    public static ProductException serverErr(String message) {
        return new ProductException(HttpStatus.BAD_REQUEST, "PRODUCT_ERR_04" , message);
    }
}
