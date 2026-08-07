package com.salofresh.repository;

import com.salofresh.common.enums.CampaignRecipientStatus;
import com.salofresh.entity.CampaignRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {

    List<CampaignRecipient> findAllByCampaignIdAndStatus(Long campaignId, CampaignRecipientStatus status);

    Page<CampaignRecipient> findAllByCampaignId(Long campaignId, Pageable pageable);

    long countByCampaignIdAndStatus(Long campaignId, CampaignRecipientStatus status);
}
