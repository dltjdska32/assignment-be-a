package com.assginment.be_a.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


/// swagger사용을 위해 사용
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 모든 도메인(ip) 허용
        // credentials(true)와 allowedOrigins("*") 조합은 스펙상 금지라서 patterns 사용
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // 허용할 http 메서드 (get / post/ put/ fetch/ delete)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));

        // 요청에 credential (쿠키 http인증해더 등등) 을 포함할지 여부
        // 서버 응답할때 json을 자바스크립트에서 처리할수 있게 할지 설정.
        config.setAllowCredentials(true);

        // 허용할 헤더 설정
        config.setAllowedHeaders(Arrays.asList("*"));

        // 모든 "/api/**" 경로에 CORS 설정 적용
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}