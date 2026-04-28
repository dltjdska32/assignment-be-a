package com.assginment.be_a.infra.jwt;



import com.assginment.be_a.infra.config.BasicUserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.assginment.be_a.infra.config.GlobalConst.AUTHORIZATION_HEADER;
import static com.assginment.be_a.infra.config.GlobalConst.AUTHORIZATION_HEADER_TYPE;


@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        BasicUserInfo basicUserInfo = getBasicUserInfo(request);

        ///  해당 필터에 거치지 않으면 게스트 유저
        if (basicUserInfo != null) {

            /// Spring Security는 단순 문자열로는 권한을 못 알아먹음
            ///  SimpleGrantedAuthority라는 전용 객체로 포장
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(basicUserInfo.role().name()));

            Authentication auth = new UsernamePasswordAuthenticationToken(basicUserInfo, null, authorities);

            ///  Spring Security는 내부적으로 돌아갈 때 Authentication 객체가 무조건 필요
            ///  세션 처럼 서버내에 계속 저장하는것이 아닌 응답을 보내고 해당 객체는 제거됨.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }


        filterChain.doFilter(request, response);
    }


    ///  토큰 검증 및 유저기본정보반환
    private BasicUserInfo getBasicUserInfo(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(AUTHORIZATION_HEADER_TYPE)) {

            String token = bearerToken.substring(7);
            jwtProvider.validateToken(token);
            return jwtProvider.getBasicUserInfo(token);
        }

        return null;
    }

}