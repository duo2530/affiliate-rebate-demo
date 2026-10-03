package com.techplant.affiliate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    WebMvcConfigurer corsConfigurer(@Value("${app.frontend-origin}") String frontendOrigin) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(frontendOrigin)
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("Content-Type", "Idempotency-Key")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
