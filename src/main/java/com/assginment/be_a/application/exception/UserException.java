package com.assginment.be_a.application.exception;

import com.assginment.be_a.infra.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class UserException extends BaseException {
    public UserException(HttpStatusCode statusCode, String code, String message) {
        super(statusCode, code, message);
    }

    public static UserException unauthorized(String message) {
        return new UserException(HttpStatus.UNAUTHORIZED, "USER_ERR_01" , message);
    }

    public static UserException badRequest(String message) {
        return new UserException(HttpStatus.BAD_REQUEST, "USER_ERR_02" , message);
    }

    public static UserException apiErr(String message) {
        return new UserException(HttpStatus.BAD_REQUEST, "USER_ERR_03" , message);
    }

    public static UserException serverErr(String message) {
        return new UserException(HttpStatus.BAD_REQUEST, "USER_ERR_04" , message);
    }
}
