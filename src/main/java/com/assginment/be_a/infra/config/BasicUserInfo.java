package com.assginment.be_a.infra.config;

import com.assginment.be_a.domain.enums.Role;

public record BasicUserInfo(Long userId,
                            String username,
                            String email,
                            Role role)  {

    public BasicUserInfo of(Long userId, String username, String email, Role role) {
        return new BasicUserInfo(userId, username, email, role);
    }

}
