package com.assginment.be_a.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum  AttendanceState {

    PENDING("신청 완료", "결제 대기"),
    CONFIRMED("수강 확정", " 결제 완료,"),
    CANCELLED("취소", "취소");

    private String value;
    private String paymentValue;
}
