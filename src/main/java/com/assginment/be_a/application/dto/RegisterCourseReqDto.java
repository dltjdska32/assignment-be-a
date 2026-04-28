package com.assginment.be_a.application.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterCourseReqDto(
        @NotNull(message = "등록할 상품 정보는 필수입니다.")
        Long productId
) {
}
