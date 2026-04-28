package com.assginment.be_a.unit.infra.jwt;

import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.infra.config.BasicUserInfo;
import com.assginment.be_a.infra.jwt.JwtProvider;
import com.assginment.be_a.infra.jwt.exception.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider();

    @BeforeEach
    void setUp() {
        /// HS256 최소 길이 충족하는 시크릿
        ReflectionTestUtils.setField(jwtProvider, "secret", "x".repeat(64));
        ReflectionTestUtils.setField(jwtProvider, "accessExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshExpiration", 120_000L);
        jwtProvider.init();
    }

    @Test
    @DisplayName("createAccessToken 후 getBasicUserInfo로 클레임 복원")
    void createAndParseAccessToken_roundTrip() {
        String token = jwtProvider.createAccessToken(42L, "kim", "kim@example.com", Role.ROLE_CLASSMATE);

        jwtProvider.validateToken(token);
        BasicUserInfo info = jwtProvider.getBasicUserInfo(token);

        assertThat(info.userId()).isEqualTo(42L);
        assertThat(info.username()).isEqualTo("kim");
        assertThat(info.email()).isEqualTo("kim@example.com");
        assertThat(info.role()).isEqualTo(Role.ROLE_CLASSMATE);
    }

    @Test
    @DisplayName("createRefreshToken 후 getUserId로 subject 복원")
    void refreshToken_roundTripUserId() {
        String rt = jwtProvider.createRefreshToken(99L);
        jwtProvider.validateToken(rt);
        assertThat(jwtProvider.getUserId(rt)).isEqualTo(99L);
    }

    @Test
    @DisplayName("잘못된 형식 토큰이면 JwtException")
    void validateToken_throwsOnMalformedToken() {
        assertThatThrownBy(() -> jwtProvider.validateToken("not-a-valid-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 액세스 토큰이면 JwtException")
    void validateToken_throwsWhenExpired() {
        ReflectionTestUtils.setField(jwtProvider, "accessExpiration", 1L);
        jwtProvider.init();

        String token = jwtProvider.createAccessToken(1L, "a", "a@b.com", Role.ROLE_CLASSMATE);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> jwtProvider.validateToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("만료");
    }
}

