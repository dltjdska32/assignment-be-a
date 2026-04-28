package com.assginment.be_a.unit.domain;

import com.assginment.be_a.application.exception.EnrollmentListException;
import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.ProductCategory;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EnrollmentListTest {

    @Test
    @DisplayName("validCancel: 마지막 수정 후 7일 이내면 통과")
    void validCancel_okWithinSevenDays() {
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        Product product = Product.builder()
                .id(1L)
                .productName("n")
                .description("d")
                .user(user)
                .productCategory(mock(ProductCategory.class))
                .cost(1L)
                .capacity(10)
                .reservedCnt(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .productState(ProductState.OPEN)
                .build();

        EnrollmentList enrollment = EnrollmentList.builder()
                .id(1L)
                .user(user)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(6));

        assertThatCode(enrollment::validCancel).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validCancel: 마지막 수정 후 7일 초과면 EnrollmentListException")
    void validCancel_throwsAfterSevenDays() {
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        Product product = Product.builder()
                .id(1L)
                .productName("n")
                .description("d")
                .user(user)
                .productCategory(mock(ProductCategory.class))
                .cost(1L)
                .capacity(10)
                .reservedCnt(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .productState(ProductState.OPEN)
                .build();

        EnrollmentList enrollment = EnrollmentList.builder()
                .id(1L)
                .user(user)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(8));

        assertThatThrownBy(enrollment::validCancel)
                .isInstanceOf(EnrollmentListException.class)
                .hasMessageContaining("취소할 수 없는 상태입니다.");
    }
}

