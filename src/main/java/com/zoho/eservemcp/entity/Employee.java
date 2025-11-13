package com.zoho.eservemcp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Data
public class Employee {

    @Id
    @Column(name = "sid", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID sid;

    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;

    @Column(name = "email", unique = true, nullable = false)
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@eservecloud\\.in$", message = "Email must be from @eservecloud.in domain")
    private String email;

    @Column(name = "zoho_erec_no", unique = true, nullable = false)
    private String zohoErecNo;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Role {
        USER,
        ADMIN
    }
}