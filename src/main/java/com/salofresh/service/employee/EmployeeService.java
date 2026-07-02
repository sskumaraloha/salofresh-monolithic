package com.salofresh.service.employee;

import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.dto.employee.EmployeeCreateRequest;
import com.salofresh.dto.employee.EmployeePerformanceResponse;
import com.salofresh.dto.employee.EmployeeResponse;
import com.salofresh.dto.employee.EmployeeUpdateRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse create(Long salonId, EmployeeCreateRequest request);

    EmployeeResponse update(Long salonId, Long employeeId, EmployeeUpdateRequest request);

    void delete(Long salonId, Long employeeId);

    EmployeeResponse getById(Long salonId, Long employeeId);

    PagedResponse<EmployeeResponse> listBySalon(Long salonId, EmploymentStatus status, Long serviceId, Pageable pageable);

    EmployeeResponse assignServices(Long salonId, Long employeeId, List<Long> serviceIds);

    EmployeeResponse uploadPhoto(Long salonId, Long employeeId, MultipartFile file);

    EmployeePerformanceResponse getPerformance(Long salonId, Long employeeId);
}
