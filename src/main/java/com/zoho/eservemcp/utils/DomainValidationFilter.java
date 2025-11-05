package com.zoho.eservemcp.utils;

import com.zoho.eservemcp.entity.Employee;
import com.zoho.eservemcp.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class DomainValidationFilter extends OncePerRequestFilter {

    private final String allowedDomain;
    private final EmployeeRepository employeeRepository;

    public DomainValidationFilter(String allowedDomain, EmployeeRepository employeeRepository) {
        this.allowedDomain = allowedDomain;
        this.employeeRepository = employeeRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        // Extract email from request header or parameter
        String emailIdentifier = extractEmailFromRequest(request);
        
        if (emailIdentifier == null || emailIdentifier.isEmpty()) {
            sendErrorResponse(response, HttpStatus.BAD_REQUEST,
                "Email identifier is required in X-Employee-Email header or email parameter");
            return;
        }
        
        // Validate domain
        if (!isValidDomain(emailIdentifier)) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                "Access denied. Only @" + allowedDomain + " domain is allowed");
            return;
        }
        
        // Validate employee exists and is active
        Optional<Employee> employee = employeeRepository.findByEmailAndIsActive(emailIdentifier, true);
        if (employee.isEmpty()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
                "Employee not found or inactive: " + emailIdentifier);
            return;
        }
        
        // Store employee in request attribute for downstream use
        request.setAttribute("validated_employee", employee.get());
        
        filterChain.doFilter(request, response);
    }
    
    private String extractEmailFromRequest(HttpServletRequest request) {
        // Check header first
        String email = request.getHeader("X-Employee-Email");
        if (email != null && !email.isEmpty()) {
            return email;
        }
        
        // Check query parameter
        email = request.getParameter("email");
        return email;
    }
    
    private boolean isValidDomain(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1);
        return allowedDomain.equalsIgnoreCase(domain);
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"%s\", \"status\": %d}", 
            message, status.value()
        ));
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter for health check endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/health");
    }
}
