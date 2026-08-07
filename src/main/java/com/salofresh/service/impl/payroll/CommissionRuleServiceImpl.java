package com.salofresh.service.impl.payroll;

import com.salofresh.dto.payroll.CommissionRuleRequest;
import com.salofresh.dto.payroll.CommissionRuleResponse;
import com.salofresh.entity.Category;
import com.salofresh.entity.CommissionRule;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.payroll.CommissionRuleMapper;
import com.salofresh.repository.CategoryRepository;
import com.salofresh.repository.CommissionRuleRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.service.payroll.CommissionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionRuleServiceImpl implements CommissionRuleService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final CommissionRuleMapper commissionRuleMapper;

    @Override
    @Transactional
    public CommissionRuleResponse create(Long ownerUserId, Long salonId, CommissionRuleRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        CommissionRule rule = commissionRuleMapper.toEntity(request);
        rule.setSalon(salon);
        rule.setEmployee(resolveEmployee(salon, request.getEmployeeId()));
        rule.setCategory(resolveCategory(request.getCategoryId()));
        rule.setActive(request.getActive() == null || request.getActive());

        CommissionRule saved = commissionRuleRepository.save(rule);
        return commissionRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommissionRuleResponse update(Long ownerUserId, Long salonId, Long ruleId, CommissionRuleRequest request) {
        CommissionRule rule = getRuleOrThrow(salonId, ruleId);
        verifyOwnership(rule.getSalon(), ownerUserId);

        commissionRuleMapper.updateEntityFromRequest(request, rule);
        rule.setEmployee(resolveEmployee(rule.getSalon(), request.getEmployeeId()));
        rule.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }

        CommissionRule saved = commissionRuleRepository.save(rule);
        return commissionRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long ownerUserId, Long salonId, Long ruleId) {
        CommissionRule rule = getRuleOrThrow(salonId, ruleId);
        verifyOwnership(rule.getSalon(), ownerUserId);
        commissionRuleRepository.delete(rule);
    }

    @Override
    public CommissionRuleResponse getById(Long ownerUserId, Long salonId, Long ruleId) {
        CommissionRule rule = getRuleOrThrow(salonId, ruleId);
        verifyOwnership(rule.getSalon(), ownerUserId);
        return commissionRuleMapper.toResponse(rule);
    }

    @Override
    public List<CommissionRuleResponse> listForSalon(Long ownerUserId, Long salonId) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        return commissionRuleRepository.findAll().stream()
                .filter(rule -> rule.getSalon() != null && rule.getSalon().getId().equals(salonId))
                .map(commissionRuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Salon getSalonOrThrow(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.isDeleted()) {
            throw new ResourceNotFoundException("Salon", "id", salonId);
        }
        return salon;
    }

    private CommissionRule getRuleOrThrow(Long salonId, Long ruleId) {
        return commissionRuleRepository.findByIdAndSalonId(ruleId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule", "id", ruleId));
    }

    private Employee resolveEmployee(Salon salon, Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salon.getId())
                .orElseThrow(() -> new BadRequestException("Employee id " + employeeId + " does not belong to this salon"));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category id " + categoryId + " does not exist"));
    }

    private void verifyOwnership(Salon salon, Long ownerUserId) {
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new ForbiddenException("You do not have permission to manage commission rules for this salon");
        }
    }
}
