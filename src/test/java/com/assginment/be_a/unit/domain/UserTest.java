package com.assginment.be_a.unit.domain;

import com.assginment.be_a.application.dto.SignUpReqDto;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final String VALID_PASSWORD = "Abcd1234!";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("from: DTO로 ROLE_CLASSMATE 유저를 만들고 비밀번호는 인코딩된다")
    void from_buildsUserWithEncodedPassword() {
        SignUpReqDto dto = new SignUpReqDto("usernm", VALID_PASSWORD, "u@example.com");

        User user = User.from(dto, passwordEncoder);

        assertThat(user.getUsername()).isEqualTo("usernm");
        assertThat(user.getEmail()).isEqualTo("u@example.com");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_CLASSMATE);
        assertThat(user.getPassword()).isNotEqualTo(VALID_PASSWORD);
        assertThat(passwordEncoder.matches(VALID_PASSWORD, user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("isValidPassword: 일치하면 예외 없음")
    void isValidPassword_okWhenMatches() {
        SignUpReqDto dto = new SignUpReqDto("usernm", VALID_PASSWORD, "u@example.com");
        User user = User.from(dto, passwordEncoder);

        user.isValidPassword(VALID_PASSWORD, passwordEncoder);
    }

    @Test
    @DisplayName("isValidPassword: 불일치하면 IllegalArgumentException")
    void isValidPassword_throwsWhenMismatch() {
        SignUpReqDto dto = new SignUpReqDto("usernm", VALID_PASSWORD, "u@example.com");
        User user = User.from(dto, passwordEncoder);

        assertThatThrownBy(() -> user.isValidPassword("Wrong12!", passwordEncoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디 또는 비밀번호가 틀렸습니다");
    }
}

