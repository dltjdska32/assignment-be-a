package com.assginment.be_a.support;

import com.assginment.be_a.infra.config.BasicUserInfo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class MockBasicUserSecurityContextFactory implements WithSecurityContextFactory<WithMockBasicUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockBasicUser annotation) {
        BasicUserInfo principal = new BasicUserInfo(
                annotation.userId(),
                annotation.username(),
                annotation.email(),
                annotation.role());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(annotation.role().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
