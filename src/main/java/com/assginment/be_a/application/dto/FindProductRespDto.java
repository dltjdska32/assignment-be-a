package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.enums.ProductState;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FindProductRespDto (
    Long productId,
    String productName,
    Long cost,
    ProductState productState,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime createdAt,
    String creatorName
) {
}
