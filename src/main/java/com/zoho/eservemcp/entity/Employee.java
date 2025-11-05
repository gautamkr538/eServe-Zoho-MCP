package com.zoho.eservemcp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;
    
    @Column(name = "email", unique = true, nullable = false)
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@eservecloud\\.in$", message = "Email must be from @eservecloud.in domain")
    private String email;
    
    @Column(name = "zoho_erec_no", unique = true, nullable = false)
    private String zohoErecNo;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}