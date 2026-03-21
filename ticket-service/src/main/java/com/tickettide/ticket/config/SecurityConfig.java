package com.tickettide.ticket.config;

import com.tickettide.common.security.InternalApiKeyFilter;
import com.tickettide.common.security.JwtAuthenticationFilter;
import com.tickettide.common.security.SecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           InternalApiKeyFilter internalFilter) throws Exception {
        return SecurityDefaults.apply(http, jwtFilter, internalFilter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/tickets/*/check-in").hasAnyRole("ORGANIZER", "ADMIN")
                        .anyRequest().authenticated())
                .build();
    }
}
