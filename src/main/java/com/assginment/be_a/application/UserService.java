package com.assginment.be_a.application;

import com.assginment.be_a.application.dto.LoginReqDto;
import com.assginment.be_a.application.dto.LoginRespDto;
import com.assginment.be_a.application.dto.SignUpReqDto;
import com.assginment.be_a.application.exception.UserException;
import com.assginment.be_a.application.port.JwtPort;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static com.assginment.be_a.infra.config.GlobalConst.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepo userRepo;
    private final JwtProvider jwtProvider;
    private final JwtPort jwtPort;
    private final PasswordEncoder passwordEncoder;


    /**
     * 회원가입.
     * @param dto
     */
    @Transactional(readOnly = false)
    public void signUp(SignUpReqDto dto) {

        if (userRepo.existsByUsername(dto.username())) {
            throw UserException.badRequest("이미 존재하는 아이디 입니다.");
        }

        User user = User.from(dto, passwordEncoder);
        userRepo.save(user);
    }


    @Transactional(readOnly = false)
    public LoginRespDto login(HttpServletResponse resp, LoginReqDto dto) {

        ///  아이디 검증.
        User user = userRepo.findByUsername(dto.username())
                .orElseThrow(() -> UserException.badRequest("아이디 또는 비밀번호가 틀렸습니다."));

        try {
            user.isValidPassword(dto.password(), passwordEncoder);
        } catch (IllegalArgumentException e) {
            throw UserException.badRequest(e.getMessage());
        }


        saveTokens(resp, user);

        return new LoginRespDto(user.getId(), user.getUsername(), user.getEmail());
    }



    @Transactional(readOnly = false)
    public void reissue(HttpServletRequest req, HttpServletResponse resp) {

        String rt = getRefreshToken(req);

        Long userId = jwtPort.findAndDeleteRefreshToken(rt)
                .orElseThrow(() -> UserException.unauthorized("토큰이 만료되었습니다. 재로그인이 필요합니다."));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> UserException.badRequest("확인할 수 없는 유저입니다."));

        saveTokens(resp, user);
    }



    @Transactional(readOnly = false)
    public void logout(HttpServletRequest req, HttpServletResponse resp) {

        String refreshToken = getRefreshToken(req);

        jwtPort.findAndDeleteRefreshToken(refreshToken);

        jwtProvider.deleteRefreshTokenFromCookie(resp);
    }


    private void saveTokens(HttpServletResponse resp, User user) {
        /// 리프레시토큰 생성 및 저장.
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        jwtPort.saveRefreshToken(user.getId(), refreshToken);

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        /// 리프레시 토큰 쿠키 저장 및 엑세스토큰 헤더 저장.
        jwtProvider.saveRefreshTokenToCookie(resp, refreshToken);
        resp.addHeader(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_TYPE + accessToken);
    }


    private static @NonNull String getRefreshToken(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();

        if (cookies == null) {
            throw UserException.badRequest("토큰을 확인할 수 없습니다.");
        }

        String rt = Arrays.stream(cookies)
                .filter(c -> c.getName().equals(REFRESH_TOKEN_COOKIE_KEY))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> UserException.badRequest("토큰을 확인할 수 없습니다."));

        return rt;
    }


}
