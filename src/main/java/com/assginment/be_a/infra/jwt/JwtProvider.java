package com.assginment.be_a.infra.jwt;


import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.infra.config.BasicUserInfo;
import com.assginment.be_a.infra.jwt.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.assginment.be_a.infra.config.GlobalConst.*;

/// MVC전용
/// 검증은 api 게이트웨이에서 진행하기 때문에 검증은 사용하지 않음.
/// 앱전용 서버일 경우 쿠키에 리프레시 토큰을 담아주는 메서드 사용하지않음.
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        initializeSecretKey();
    }

    /// 개인키 생성.
    /// byte의 크기에 따라 암호화 알고리즘 결정.
    private void initializeSecretKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * accessToken 생성
     */
    public String createAccessToken(Long userId, String userName, String email, Role role) {
        long now = System.currentTimeMillis();
        Date accessTokenExpiration = new Date(now + accessExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(JWT_CLAIM_ROLE, role.name())     // 사용자 정의 클레임 (문자열로 고정)
                .claim(JWT_CLAIM_USERNAME, userName)    // 사용자 정의 클레임
                .claim(JWT_CLAIM_EMAIL, email)          // 사용자 정의 클레임
                .issuedAt(new Date(now))
                .expiration(accessTokenExpiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * refreshToken 생성
     */
    public String createRefreshToken(Long memberId) {
        long now = System.currentTimeMillis();
        Date refreshTokenExpiration = new Date(now + refreshExpiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiration)
                .signWith(secretKey)
                .compact();
    }



    /**
     * refreshToken을 쿠키에 저장 - 앱일경우 사용 x
     */
    public void saveRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_KEY, refreshToken);
        cookie.setHttpOnly(true);                                       // httpOnly 설정 - js 접근 불가
        cookie.setSecure(false);                                        // https 강제  -  개발 : false , 배포 -true
        cookie.setPath("/");                                            // 모든 경로에 쿠키포함.
        cookie.setMaxAge(Math.toIntExact(refreshExpiration / 1000));    //쿠키 만료 시간
        response.addCookie(cookie);

    }

    /**
     * 쿠키에서 refreshToken 삭제 - 앱일경우 사용 x
     */
    public void deleteRefreshTokenFromCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_KEY, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * 토큰 검증
     * @param token
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (SecurityException | MalformedJwtException e) {
            throw JwtException.jwtInvalidMalformedEx("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            throw JwtException.jwtExpiredEx("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            throw JwtException.jwtUnsupportedEx("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            throw JwtException.jwtClaimEmptyEx("JWT 토큰이 잘못되었습니다.");
        } catch (Exception e) {
            throw JwtException.jwtInvalidEx("유효하지 않은 JWT 토큰입니다.");
        }
    }



    /**
     * Token Claims 가져오기
     */
    public Claims getClaims(String token) {

        validateToken(token);

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public BasicUserInfo getBasicUserInfo(String token) {

        Claims claims = getClaims(token);

        String userId = claims.getSubject();
        String userRole = claims.get(JWT_CLAIM_ROLE, String.class);
        String userEmail = claims.get(JWT_CLAIM_EMAIL, String.class);
        String username = claims.get(JWT_CLAIM_USERNAME, String.class);

        if(userId == null || userRole == null || username == null || userEmail == null){
            throw JwtException.jwtClaimEmptyEx("JWT 유저 정보를 확인할 수 없음.");
        }

        return new BasicUserInfo(Long.valueOf(userId),
                username,
                userEmail,
                Role.valueOf(userRole));
    }

    /// 유저ID 확인
    public Long getUserId(String token){

        Claims claims = getClaims(token);

        String userId = claims.getSubject();

        if(userId == null){
            throw JwtException.jwtClaimEmptyEx("JWT 유저 정보를 확인할 수 없음.");
        }

        return Long.parseLong(userId);
    }


    /// 유저롤 확인
    public String getUserRole(String token){

        Claims claims = getClaims(token);
        String userRole = claims.get(JWT_CLAIM_ROLE, String.class);

        if(userRole == null){
            throw JwtException.jwtClaimEmptyEx("JWT 유저 정보를 확인할 수 없음.");
        }

        return userRole;
    }


    /// 유저email 확인
    public String getUserEmail(String token){

        Claims claims = getClaims(token);
        String userEmail = claims.get(JWT_CLAIM_EMAIL, String.class);

        if(userEmail == null){
            throw JwtException.jwtClaimEmptyEx("JWT 유저 정보를 확인할 수 없음.");
        }

        return userEmail;
    }

    /// 유저네임 확인
    public String getUsername(String token){

        Claims claims = getClaims(token);
        String username = claims.get(JWT_CLAIM_USERNAME, String.class);

        if(username == null){
            throw JwtException.jwtClaimEmptyEx("JWT 유저 정보를 확인할 수 없음.");
        }

        return username;
    }

}
