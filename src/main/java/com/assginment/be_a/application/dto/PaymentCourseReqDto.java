package com.assginment.be_a.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCourseReqDto (
        @NotNull(message = "결제 상품ID는 필수입니다.")
        Long enrollmentId
        ){
}
