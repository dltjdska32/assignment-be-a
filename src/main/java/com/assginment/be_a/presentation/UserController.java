package com.assginment.be_a.presentation;

import com.assginment.be_a.application.UserService;
import com.assginment.be_a.application.dto.LoginReqDto;
import com.assginment.be_a.application.dto.LoginRespDto;
import com.assginment.be_a.application.dto.SignUpReqDto;
import com.assginment.be_a.infra.config.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpRequest;

@RestController
@RequiredArgsConstructor
@Tag(name = "USER-API", description = "유저 관련 API 엔드포인트")
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    @Operation(
            summary = "회원가입",
            description = "유저는 아이디, 패스워드, 이메일을 통해 회원가입이 가능하다."
    )
    public Response<Void> signUp(@Valid @RequestBody SignUpReqDto dto) {

        userService.signUp(dto);

        return Response.ok();

    }


    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "유저는 아이디, 패스워드 로그인이 가능하다."
    )
    public Response<LoginRespDto> login(HttpServletResponse resp,
                                        @Valid @RequestBody LoginReqDto dto) {

        LoginRespDto respVal = userService.login(resp, dto);

        return Response.ok(respVal);
    }

    @PostMapping("/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = "유저는 토큰이 만료되면 재발급을 받는다 (RTR 방식)"
    )
    public Response<Void> reissue (HttpServletRequest req,
                                   HttpServletResponse resp) {

        userService.reissue(req, resp);

        return Response.ok();
    }


    @Operation(
            summary = "로그아웃",
            description = "로그아웃 "
    )
    @PostMapping("/logout")
    public Response<Void> logout(HttpServletRequest req,
                                   HttpServletResponse resp) {

        ///  레디스에서 제거
        userService.logout(req, resp);

        return Response.ok();
    }
}
