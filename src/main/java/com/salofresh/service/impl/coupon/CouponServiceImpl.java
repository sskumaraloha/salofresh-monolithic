package com.salofresh.service.impl.coupon;

import com.salofresh.coupon.CouponEngine;
import com.salofresh.coupon.CouponValidationResult;
import com.salofresh.dto.coupon.CouponCreateRequest;
import com.salofresh.dto.coupon.CouponResponse;
import com.salofresh.dto.coupon.CouponUpdateRequest;
import com.salofresh.dto.coupon.ValidateCouponRequest;
import com.salofresh.dto.coupon.ValidateCouponResponse;
import com.salofresh.entity.Coupon;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.coupon.CouponMapper;
import com.salofresh.repository.CouponRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final SalonRepository salonRepository;
    private final UserRepository userRepository;
    private final CouponMapper couponMapper;
    private final CouponEngine couponEngine;

    @Override
    @Transactional
    public CouponResponse create(CouponCreateRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("A coupon with code '" + request.getCode() + "' already exists");
        }
        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw new BadRequestException("validTo must be after validFrom");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .usageLimit(request.getUsageLimit())
                .usagePerUser(request.getUsagePerUser() != null ? request.getUsagePerUser() : 1)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .description(request.getDescription())
                .active(true)
                .applicableSalon(resolveSalon(request.getApplicableSalonId()))
                .build();

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse update(Long id, CouponUpdateRequest request) {
        Coupon coupon = getCouponOrThrow(id);

        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(coupon.getCode())
                && couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("A coupon with code '" + request.getCode() + "' already exists");
        }

        if (request.getCode() != null) {
            coupon.setCode(request.getCode().toUpperCase());
        }
        if (request.getType() != null) {
            coupon.setType(request.getType());
        }
        if (request.getDiscountValue() != null) {
            coupon.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getMinOrderAmount() != null) {
            coupon.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getUsageLimit() != null) {
            coupon.setUsageLimit(request.getUsageLimit());
        }
        if (request.getUsagePerUser() != null) {
            coupon.setUsagePerUser(request.getUsagePerUser());
        }
        if (request.getValidFrom() != null) {
            coupon.setValidFrom(request.getValidFrom());
        }
        if (request.getValidTo() != null) {
            coupon.setValidTo(request.getValidTo());
        }
        if (coupon.getValidTo() != null && coupon.getValidFrom() != null && !coupon.getValidTo().isAfter(coupon.getValidFrom())) {
            throw new BadRequestException("validTo must be after validFrom");
        }
        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
        }
        if (request.getApplicableSalonId() != null) {
            coupon.setApplicableSalon(resolveSalon(request.getApplicableSalonId()));
        }
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return couponMapper.toResponse(getCouponOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CouponResponse> list(Pageable pageable, boolean activeOnly) {
        Page<Coupon> page = activeOnly ? couponRepository.findAllByActiveTrue(pageable) : couponRepository.findAll(pageable);
        List<CouponResponse> content = page.getContent().stream().map(couponMapper::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Coupon coupon = getCouponOrThrow(id);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateCouponResponse validate(ValidateCouponRequest request, Long currentUserId) {
        try {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
            CouponValidationResult result = couponEngine.validateAndCalculate(
                    request.getCode(), user, request.getSalonId(), request.getOrderAmount());
            return ValidateCouponResponse.builder()
                    .valid(true)
                    .discountAmount(result.discountAmount())
                    .message("Coupon applied successfully")
                    .build();
        } catch (BadRequestException e) {
            return ValidateCouponResponse.builder()
                    .valid(false)
                    .discountAmount(BigDecimal.ZERO)
                    .message(e.getMessage())
                    .build();
        }
    }

    private Coupon getCouponOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }

    private Salon resolveSalon(Long salonId) {
        if (salonId == null) {
            return null;
        }
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }
}
