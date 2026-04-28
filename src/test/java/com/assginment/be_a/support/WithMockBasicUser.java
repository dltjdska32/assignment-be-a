package com.assginment.be_a.support;

import com.assginment.be_a.domain.enums.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link com.assginment.be_a.infra.config.BasicUserInfo}를 Principal로 두는 슬라이스/MockMvc 테스트용.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockBasicUserSecurityContextFactory.class)
public @interface WithMockBasicUser {

    long userId() default 1L;

    String username() default "tester";

    String email() default "tester@example.com";

    Role role() default Role.ROLE_CLASSMATE;
}
