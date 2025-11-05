package com.zoho.eservemcp.repository;

import com.zoho.eservemcp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailAndIsActive(String email, Boolean isActive);

    Optional<Employee> findByEmployeeIdAndIsActive(String employeeId, Boolean isActive);

    @Query("SELECT e FROM Employee e WHERE (e.email = :identifier OR e.employeeId = :identifier) AND e.isActive = true")
    Optional<Employee> findByIdentifierAndActive(@Param("identifier") String identifier);
}