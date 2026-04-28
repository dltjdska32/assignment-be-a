package com.assginment.be_a.infra.jwt.exception;

import com.assginment.be_a.infra.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;


/// 403에러 제외
/// 401에러를 반환하게 하여 토큰 재발급 유도.
public class JwtException extends BaseException {

    public JwtException(HttpStatusCode statusCode, String code, String message) {
        super(statusCode, code, message);
    }

    public static JwtException jwtInvalidMalformedEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_01" , message);
    }

    public static JwtException jwtExpiredEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_02" , message);
    }

    public static JwtException jwtClaimEmptyEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_03" , message);
    }

    public static JwtException jwtUnsupportedEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_04" , message);
    }

    public static JwtException jwtInvalidEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_05" , message);
    }

    public static JwtException notFoundEx(String message) {
        return new JwtException(HttpStatus.UNAUTHORIZED, "JWT_ERR_06" , message);
    }

    ///  403에러 권한 없음.
    public static JwtException accessDeniedEx(String message) {
        return new JwtException(HttpStatus.FORBIDDEN, "JWT_ERR_07", message);
    }
}
