package com.assginment.be_a.application.dto;

import com.assginment.be_a.domain.ProductCategory;
import com.assginment.be_a.domain.enums.ProductState;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

public record CreateProductReqDto(
        @NotBlank(message = "상품(강의)명은 필수 입니다.")
        String productName,
        @Length(max = 5000, message = "설명은 최대 5000자 입니다.")
        String description,
        @NotNull(message = "상품카테고리는 필수 입니다.")
        ProductCategory productCategory,
        @Positive(message = "가격은 0이상이어야 합니다.")
        Long cost,
        @Positive(message = "정원은 0이상이어야 합니다.")
        Integer capacity,
        LocalDateTime startDate,
        LocalDateTime endDate
) {

    /**
     * 날짜 필수 여부 검증
     */
    @AssertTrue(message = "시작일과 종료일은 필수입니다.")
    public boolean isDateRequired() {
        return startDate != null && endDate != null;
    }

    /**
     * 날짜 검증
     */
    @AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

}
