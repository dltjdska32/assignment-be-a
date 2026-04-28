package com.assginment.be_a.infra.jwt;

import com.assginment.be_a.infra.config.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 특정 API엔 권한이 필요하지만 그 권한을 만족하지 못했을때 AccessDeniedException이 발생한다. 403 에러처리
     * @param request
     * @param response
     * @param accessDeniedException
     * @throws IOException
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValue(response.getWriter()
                , Response.error(HttpStatus.FORBIDDEN, "JWT_ACCESS_DENIED", "요청한 항목에 대한 권한이 없습니다."));
    }
}