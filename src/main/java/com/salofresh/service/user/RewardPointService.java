package com.salofresh.service.user;

import com.salofresh.dto.user.RewardPointResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface RewardPointService {

    PagedResponse<RewardPointResponse> getHistory(Long userId, Pageable pageable);

    int getCurrentBalance(Long userId);
}
