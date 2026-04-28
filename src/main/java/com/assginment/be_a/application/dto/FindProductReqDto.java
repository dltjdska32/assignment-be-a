package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.enums.ProductState;

import java.time.LocalDateTime;

public record FindProductReqDto (
        Long categoryId,
        Long cursorId,
        LocalDateTime cursorCreatedAt,
        ProductState productState
) {
}
