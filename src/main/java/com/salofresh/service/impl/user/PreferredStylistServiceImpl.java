package com.salofresh.service.impl.user;

import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.entity.Customer;
import com.salofresh.entity.Employee;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.CustomerRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.service.booking.BookingService;
import com.salofresh.service.user.PreferredStylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreferredStylistServiceImpl implements PreferredStylistService {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BookingService bookingService;

    @Override
    @Transactional
    public void setPreferredStylist(Long userId, Long employeeId) {
        Customer customer = getCustomer(userId);
        Employee employee = getEmployee(employeeId);
        customer.setPreferredEmployeeId(employee.getId());
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getPreferredStylist(Long userId) {
        Customer customer = getCustomer(userId);
        if (customer.getPreferredEmployeeId() == null) {
            return null;
        }
        return employeeRepository.findById(customer.getPreferredEmployeeId())
                .filter(employee -> !employee.isDeleted())
                .orElse(null);
    }

    @Override
    @Transactional
    public void clearPreferredStylist(Long userId) {
        Customer customer = getCustomer(userId);
        customer.setPreferredEmployeeId(null);
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CreateBookingRequest quickRebook(Long userId, Long appointmentId) {
        CreateBookingRequest template = bookingService.rebook(appointmentId);
        if (template.getEmployeeId() == null) {
            Employee preferred = getPreferredStylist(userId);
            if (preferred != null) {
                template.setEmployeeId(preferred.getId());
            }
        }
        return template;
    }

    private Customer getCustomer(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "userId", userId));
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(employee -> !employee.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }
}
