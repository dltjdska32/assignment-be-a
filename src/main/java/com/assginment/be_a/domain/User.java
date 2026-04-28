package com.assginment.be_a.domain;

import com.assginment.be_a.application.dto.SignUpReqDto;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.infra.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;



    /// origin 사용자 생성
    public static User from(SignUpReqDto dto, PasswordEncoder passwordEncoder){


        String encodedPassword = passwordEncoder.encode(dto.password());

        return User.builder()
                .email(dto.email())
                .username(dto.username())
                .password(encodedPassword)
                .role(Role.ROLE_CLASSMATE)
                .build();
    }



    public void isValidPassword(String inputPass, @NonNull PasswordEncoder encoder){

        if (!encoder.matches(inputPass, this.password)) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다.");
        }
    }

}
