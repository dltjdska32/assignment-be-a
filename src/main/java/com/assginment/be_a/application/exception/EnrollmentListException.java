package com.assginment.be_a.application.exception;

import com.assginment.be_a.infra.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class EnrollmentListException extends BaseException {

    public EnrollmentListException(HttpStatusCode statusCode, String code, String message) {
        super(statusCode, code, message);
    }



    public static EnrollmentListException unauthorized(String message) {
        return new EnrollmentListException(HttpStatus.UNAUTHORIZED, "ENROLLMENT_ERR_01" , message);
    }

    public static EnrollmentListException badRequest(String message) {
        return new EnrollmentListException(HttpStatus.BAD_REQUEST, "ENROLLMENT_ERR_02" , message);
    }

    public static EnrollmentListException apiErr(String message) {
        return new EnrollmentListException(HttpStatus.BAD_REQUEST, "ENROLLMENT_ERR_03" , message);
    }

    public static EnrollmentListException serverErr(String message) {
        return new EnrollmentListException(HttpStatus.BAD_REQUEST, "ENROLLMENT_ERR_04" , message);
    }
}
