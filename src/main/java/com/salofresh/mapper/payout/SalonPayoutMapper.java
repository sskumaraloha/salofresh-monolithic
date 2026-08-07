package com.salofresh.mapper.payout;

import com.salofresh.dto.payout.SalonPayoutResponse;
import com.salofresh.entity.SalonPayout;
import com.salofresh.mapper.salon.SalonMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", uses = SalonMapper.class)
public interface SalonPayoutMapper {

    @Mapping(target = "salon", source = "salon")
    @Mapping(target = "commissionPercentageApplied", expression = "java(calculateCommissionPercentage(salonPayout))")
    SalonPayoutResponse toResponse(SalonPayout salonPayout);

    /**
     * The entity persists only the computed money amounts, not the percentage itself, so the
     * percentage actually applied is reconstructed for display from the stored gross revenue
     * and commission amount.
     */
    default BigDecimal calculateCommissionPercentage(SalonPayout salonPayout) {
        BigDecimal grossRevenue = salonPayout.getGrossRevenue();
        BigDecimal commissionAmount = salonPayout.getPlatformCommissionAmount();
        if (grossRevenue == null || commissionAmount == null || grossRevenue.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return commissionAmount.multiply(BigDecimal.valueOf(100))
                .divide(grossRevenue, 2, RoundingMode.HALF_UP);
    }
}
