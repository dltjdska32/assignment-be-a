package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.enums.ProductState;

import java.time.LocalDateTime;

public record FindEnrollmentReqDto(Long cursorId,
                                   LocalDateTime cursorCreatedAt) {
}
