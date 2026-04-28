package com.assginment.be_a.infra.config;

import com.assginment.be_a.infra.jwt.JwtAccessDeniedHandler;
import com.assginment.be_a.infra.jwt.JwtAuthenticationEntryPoint;
import com.assginment.be_a.infra.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)//secured 어노테이션, preAuthorize어노테이션 활성화
public class SecurityConfig {

    private final String[] WHITE_LIST = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",

            "/users/join/**",
            "/users/logout/**",
            "/users/login/**",
            "/users/reissue/**",
            "/users/exists/**",
    };

    private final CorsConfigurationSource corsConfigurationSource;

    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        /// 토큰 파싱 및 검증.
        httpSecurity.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // cors설정
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource));

        //세션 사용 x
        httpSecurity.sessionManagement((session) -> {
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });


        // bearer 방식 사용을 위해
        // headers
        // Authorization: basic (ID, PW) <-> Authorization: bearer (토큰)
        httpSecurity.httpBasic((httpbasic) -> {
            httpbasic.disable();
        });


        httpSecurity.formLogin((formLogin) -> {
            formLogin.disable();
        });



        httpSecurity.csrf((auth) ->
                auth.disable()
        );


        httpSecurity.exceptionHandling(exh -> {
            exh
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint) //인증되지 않은 사용자가 보호된 리소스에 액세스 할 때 호출
                    .accessDeniedHandler(jwtAccessDeniedHandler); //권한이 없는 사용자가 보호된 리소스에 액세스 할 때 호출
        });

        httpSecurity.authorizeHttpRequests(auth -> auth // 요청에 대한 인증 설정
                .requestMatchers(WHITE_LIST).permitAll()
                .anyRequest().authenticated());  //이외의 요청은 전부 인증 필요

        return httpSecurity.build();
    }



}
