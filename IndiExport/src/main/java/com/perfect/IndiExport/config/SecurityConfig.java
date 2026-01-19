package com.perfect.IndiExport.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // ================= PUBLIC =================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/products/browse").permitAll()
                        // .requestMatchers("/api/products/countries").permitAll()
                        // .requestMatchers("/api/products/*").permitAll() // product details

                        // ================= BUYER =================
                        .requestMatchers("/api/buyer/**").hasRole("BUYER")

                        // ================= CHAT / INQUIRY / RFQ =================
                        .requestMatchers("/api/inquiries/**").hasAnyRole("BUYER", "SELLER",
                                "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers("/api/rfqs/**").hasAnyRole("BUYER", "SELLER",
                                "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers("/api/chat/**").hasAnyRole("BUYER", "SELLER",
                                "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers("/api/orders/**").hasAnyRole("BUYER", "SELLER",
                                "SELLER_ADVANCED", "ADMIN")

                        // ================= SELLER =================
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "SELLER_ADVANCED",
                                "ADMIN")
                        .requestMatchers("/api/products/my").hasAnyRole("SELLER", "SELLER_ADVANCED",
                                "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasAnyRole("SELLER", "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                        .hasAnyRole("SELLER", "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasAnyRole("SELLER", "SELLER_ADVANCED", "ADMIN")
                        .requestMatchers("/api/analytics/**").hasAnyRole("SELLER", "SELLER_ADVANCED",
                                "ADMIN")
                        .requestMatchers("/api/invoices/**").hasAnyRole("SELLER", "SELLER_ADVANCED",
                                "ADMIN")

                        // ================= ADMIN =================
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ================= DEFAULT =================
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        http.cors(Customizer.withDefaults());
        return http.build();
    }

}