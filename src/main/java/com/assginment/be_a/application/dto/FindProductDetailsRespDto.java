package com.assginment.be_a.application.dto;

import java.time.LocalDateTime;

public record FindProductDetailsRespDto (
        Long productId,
        Long creatorId,
        String creatorUsername,
        String creatorEmail,
        String productName,
        String productDescription,
        Long productCost,
        Integer productCapacity,
        Integer productReservedCnt,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt
) {
}
