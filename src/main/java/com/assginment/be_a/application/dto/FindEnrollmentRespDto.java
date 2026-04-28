package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.ProductState;

import java.time.LocalDateTime;

public record FindEnrollmentRespDto(Long productId,
                                    Long enrollmentId,
                                    String productName,
                                    Long cost,
                                    ProductState productState,
                                    AttendanceState attendanceState,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate,
                                    LocalDateTime createdAt) {
}
