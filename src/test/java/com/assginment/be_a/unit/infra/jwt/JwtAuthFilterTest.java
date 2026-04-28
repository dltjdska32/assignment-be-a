package com.assginment.be_a.unit.infra.jwt;

import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.infra.config.BasicUserInfo;
import com.assginment.be_a.infra.config.GlobalConst;
import com.assginment.be_a.infra.jwt.JwtAuthFilter;
import com.assginment.be_a.infra.jwt.JwtProvider;
import com.assginment.be_a.infra.jwt.exception.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 없으면 게스트로 통과·SecurityContext 비어 있음")
    void noHeader_passesWithoutAuthentication() throws ServletException, IOException {
        JwtAuthFilter filter = new JwtAuthFilter(jwtProvider);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        verify(jwtProvider, never()).validateToken(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 토큰이면 검증 후 SecurityContext에 BasicUserInfo 설정")
    void bearerToken_setsAuthentication() throws ServletException, IOException {
        JwtAuthFilter filter = new JwtAuthFilter(jwtProvider);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(GlobalConst.AUTHORIZATION_HEADER, GlobalConst.AUTHORIZATION_HEADER_TYPE + "my-token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        BasicUserInfo expected = new BasicUserInfo(7L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        doNothing().when(jwtProvider).validateToken("my-token");
        when(jwtProvider.getBasicUserInfo("my-token")).thenReturn(expected);

        filter.doFilter(req, res, filterChain);

        verify(jwtProvider).validateToken("my-token");
        verify(jwtProvider).getBasicUserInfo("my-token");
        verify(filterChain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(expected);
    }

    @Test
    @DisplayName("validateToken 실패 시 필터에서 예외 전파")
    void invalidToken_propagatesException() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtProvider);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(GlobalConst.AUTHORIZATION_HEADER, GlobalConst.AUTHORIZATION_HEADER_TYPE + "bad");
        MockHttpServletResponse res = new MockHttpServletResponse();

        doThrow(JwtException.jwtInvalidEx("invalid"))
                .when(jwtProvider).validateToken("bad");

        assertThatThrownBy(() -> filter.doFilter(req, res, filterChain))
                .isInstanceOf(JwtException.class);

        verify(filterChain, never()).doFilter(any(), any());
    }
}

