package com.salofresh.service.payroll;

import com.salofresh.dto.payroll.GeneratePayrollRequest;
import com.salofresh.dto.payroll.PayrollRecordResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface PayrollService {

    PayrollRecordResponse generatePayroll(Long ownerUserId, Long salonId, GeneratePayrollRequest request);

    PayrollRecordResponse finalizePayroll(Long ownerUserId, Long payrollRecordId);

    PayrollRecordResponse markPaid(Long ownerUserId, Long payrollRecordId);

    PagedResponse<PayrollRecordResponse> listForSalon(Long ownerUserId, Long salonId, Pageable pageable);

    PagedResponse<PayrollRecordResponse> listForEmployee(Long currentUserId, Long employeeId, Pageable pageable);

    PayrollRecordResponse getById(Long ownerUserId, Long salonId, Long payrollRecordId);
}
