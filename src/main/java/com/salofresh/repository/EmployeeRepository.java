package com.salofresh.repository;

import com.salofresh.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    List<Employee> findAllBySalonIdAndDeletedFalse(Long salonId);

    Optional<Employee> findByIdAndSalonIdAndDeletedFalse(Long id, Long salonId);

    List<Employee> findAllByServices_IdAndDeletedFalse(Long serviceId);
}
