package com.rcgc.illdan.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {


    private static final String[] SWAGGER_URLS = {
            "/swagger-ui/**", "/v3/api-docs/**"
    };

    private static final String[] PUBLIC_URLS = {
            "**"
    };

    private static final String[] AUTHENTICATED_URLS = {
    };

    private static final String[] ALLOWED_ORIGINS = {
            "**"
    };

    /**
     * CORS 설정을 구성하는 메서드.
     *
     * 허용된 Origin, Method, Header 등을 설정하고, 인증 관련 헤더를 노출합니다.
     *
     * @return CORS 설정 객체
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Set-Cookie"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Spring Security의 필터 체인을 구성하는 메서드.
     *
     * - HTTP 기본 인증 비활성화
     * - CSRF 비활성화
     * - CORS 설정 적용
     * - 요청 경로별 인증 정책 설정
     * - JwtFilter를 Security Filter Chain에 추가
     *
     * @param httpSecurity HttpSecurity 객체
     * @return SecurityFilterChain 객체
     * @throws Exception 필터 체인 구성 실패 시 예외 발생
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .httpBasic(HttpBasicConfigurer::disable)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(AUTHENTICATED_URLS).authenticated()
                        .anyRequest().authenticated()
                );

        return httpSecurity.build();
    }
}
