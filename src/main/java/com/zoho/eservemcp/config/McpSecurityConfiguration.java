package com.zoho.eservemcp.config;

import com.zoho.eservemcp.repository.EmployeeRepository;
import com.zoho.eservemcp.utils.DomainValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class McpSecurityConfiguration {

    @Bean
    public DomainValidationFilter domainValidationFilter(
            EmployeeRepository employeeRepository,
            @Value("${app.allowed-domain}") String allowedDomain
    ) {
        return new DomainValidationFilter(allowedDomain, employeeRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DomainValidationFilter domainValidationFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/mcp/sse", "/mcp/messages",
                                "/api/auth/register", "/api/auth/login",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(domainValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}