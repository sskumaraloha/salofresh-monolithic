package com.salofresh.controller.marketing;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.marketing.CampaignRecipientResponse;
import com.salofresh.dto.marketing.CampaignResponse;
import com.salofresh.dto.marketing.CreateCampaignRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.marketing.MarketingCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon-owner marketing broadcast tools: create/send/cancel campaigns targeting a resolved
 * customer segment (all, VIP, inactive or a custom list) over email/SMS/WhatsApp/push/in-app.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Marketing Campaigns", description = "Salon owner tools for broadcasting marketing campaigns to customer segments")
public class MarketingCampaignController {

    private final MarketingCampaignService marketingCampaignService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a new marketing campaign for a salon (owner only)",
            description = "Resolves the target audience into a customer list and creates the campaign. "
                    + "If scheduledAt is null (or in the past) the campaign is dispatched immediately; "
                    + "otherwise it is left SCHEDULED for CampaignDispatchScheduler to pick up.")
    public ResponseEntity<ApiResponse<CampaignResponse>> create(@PathVariable Long salonId,
                                                                 @Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = marketingCampaignService.create(securityUtils.getCurrentUserId(), salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Campaign created successfully", response));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send a DRAFT or SCHEDULED campaign immediately (owner only)")
    public ResponseEntity<ApiResponse<CampaignResponse>> send(@PathVariable Long salonId, @PathVariable Long id) {
        CampaignResponse response = marketingCampaignService.sendNow(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Campaign dispatched successfully", response));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a DRAFT or SCHEDULED campaign (owner only)")
    public ResponseEntity<ApiResponse<CampaignResponse>> cancel(@PathVariable Long salonId, @PathVariable Long id) {
        CampaignResponse response = marketingCampaignService.cancel(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Campaign cancelled successfully", response));
    }

    @GetMapping
    @Operation(summary = "List marketing campaigns for a salon (owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> list(
            @PathVariable Long salonId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<CampaignResponse> response =
                marketingCampaignService.listForSalon(securityUtils.getCurrentUserId(), salonId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Campaigns retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a marketing campaign by id, including recipient counts (owner only)")
    public ResponseEntity<ApiResponse<CampaignResponse>> getById(@PathVariable Long salonId, @PathVariable Long id) {
        CampaignResponse response = marketingCampaignService.getById(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Campaign retrieved successfully", response));
    }

    @GetMapping("/{id}/recipients")
    @Operation(summary = "List the recipients of a marketing campaign (owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignRecipientResponse>>> listRecipients(
            @PathVariable Long salonId, @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<CampaignRecipientResponse> response =
                marketingCampaignService.listRecipients(securityUtils.getCurrentUserId(), id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Campaign recipients retrieved successfully", response));
    }
}
