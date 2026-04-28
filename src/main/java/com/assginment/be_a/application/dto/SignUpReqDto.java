package com.assginment.be_a.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record SignUpReqDto(@NotBlank(message = "아이디는 필수 입니다.")
                                @Length(min = 5, max = 30)
                                String username,
                           @NotBlank(message = "패스워드는 필수 입니다.")
                                @Pattern(
                                        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,30}$",
                                        message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8~30자여야 합니다."
                                )
                                String password,
                           @NotBlank(message = "이메일은 필수 입력값입니다.")
                                @Email(message = "이메일 형식이 올바르지 않습니다.")
                                String email){
        }