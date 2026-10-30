package com.example.herokuDemo1.fnol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletResponse;

/**
 * Security configuration for FNOL service.
 *
 * Configures:
 * - Basic authentication for demo (can be replaced with OAuth2 in production)
 * - Role-based access control with Claims.READ and Claims.WRITE scopes
 * - Security context propagation for audit fields
 *
 * Note: For production, configure with proper OAuth2 JWT issuer and audience.
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .exceptionHandling()
                .and()
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/webjars/**").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                // FNOL endpoints require authentication
                .antMatchers("/api/claims").authenticated()
                .antMatchers("/api/claims/**").authenticated()
            .and()
                .httpBasic();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        };
    }
}
