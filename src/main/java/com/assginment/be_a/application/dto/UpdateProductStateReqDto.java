package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.enums.ProductState;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStateReqDto(
        @NotNull(message = "상품(강의) 아이디는 필수입니다.")
        Long productId,
        @NotNull(message = "상품(강의) 상태는 필수입니다.")
        ProductState productState
) {
}
