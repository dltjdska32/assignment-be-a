package com.assginment.be_a.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {

        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("CREATOR")
                .role("CREATOR").implies("CLASSMATE")
                .build();
    }

}