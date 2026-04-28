package com.assginment.be_a.domain.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProductState {

    DRAFT("초안"),
    OPEN("모집 중"),
    CLOSED("모집 마감");

    private final String value;
}
