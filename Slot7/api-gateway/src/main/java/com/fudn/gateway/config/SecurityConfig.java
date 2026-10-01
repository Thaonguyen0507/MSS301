package com.fudn.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // ==========================================================
    // TODO GW-4 – Tạo SecurityFilterChain để bảo vệ API Gateway
    // ----------------------------------------------------------
    // YÊU CẦU:
    //   1. Yêu cầu xác thực cho MỌI request (.anyRequest().authenticated())
    //   2. Cấu hình API Gateway làm OAuth2 Resource Server sử dụng JWT:
    //        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
    //   3. Build và return SecurityFilterChain
    //
    // GỢI Ý:
    //   @Bean
    //   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //       return http
    //               .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    //               .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
    //               .build();
    //   }
    //
    // IMPORT cần thêm:
    //   import org.springframework.security.config.Customizer;
    // ==========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
