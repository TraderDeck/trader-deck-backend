//package com.traderdeck.backend.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//
//
////This is to disable authentication for now
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // Allow all requests
//                .csrf(csrf -> csrf.disable()); // Disable CSRF for testing
//    }
//}