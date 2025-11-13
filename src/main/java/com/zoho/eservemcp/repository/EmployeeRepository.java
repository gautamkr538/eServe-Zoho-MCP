package com.zoho.eservemcp.repository;

import com.zoho.eservemcp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findBySid(UUID sid);

    Optional<Employee> findByEmailAndIsActive(String email, Boolean isActive);
}
