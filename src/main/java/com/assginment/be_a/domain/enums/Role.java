package com.assginment.be_a.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Role {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_CREATOR("ROLE_CREATOR"),
    ROLE_CLASSMATE("ROLE_CLASSMATE");

    private final String value;
}
