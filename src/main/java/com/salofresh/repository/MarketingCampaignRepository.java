package com.salofresh.repository;

import com.salofresh.entity.MarketingCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Long> {

    Page<MarketingCampaign> findAllBySalonIdOrderByCreatedAtDesc(Long salonId, Pageable pageable);

    Optional<MarketingCampaign> findByIdAndSalonId(Long id, Long salonId);
}
