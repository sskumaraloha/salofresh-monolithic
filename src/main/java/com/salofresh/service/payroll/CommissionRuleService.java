package com.salofresh.service.payroll;

import com.salofresh.dto.payroll.CommissionRuleRequest;
import com.salofresh.dto.payroll.CommissionRuleResponse;

import java.util.List;

public interface CommissionRuleService {

    CommissionRuleResponse create(Long ownerUserId, Long salonId, CommissionRuleRequest request);

    CommissionRuleResponse update(Long ownerUserId, Long salonId, Long ruleId, CommissionRuleRequest request);

    void delete(Long ownerUserId, Long salonId, Long ruleId);

    CommissionRuleResponse getById(Long ownerUserId, Long salonId, Long ruleId);

    List<CommissionRuleResponse> listForSalon(Long ownerUserId, Long salonId);
}
