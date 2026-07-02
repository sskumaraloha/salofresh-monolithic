package com.salofresh.service.impl.user;

import com.salofresh.dto.user.RewardPointResponse;
import com.salofresh.entity.RewardPoint;
import com.salofresh.mapper.user.RewardPointMapper;
import com.salofresh.repository.RewardPointRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.user.RewardPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardPointServiceImpl implements RewardPointService {

    private final RewardPointRepository rewardPointRepository;
    private final RewardPointMapper rewardPointMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RewardPointResponse> getHistory(Long userId, Pageable pageable) {
        Page<RewardPoint> page = rewardPointRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<RewardPointResponse> content = page.getContent().stream()
                .map(rewardPointMapper::toResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentBalance(Long userId) {
        Page<RewardPoint> latest = rewardPointRepository.findAllByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        return latest.getContent().stream()
                .findFirst()
                .map(RewardPoint::getBalanceAfter)
                .orElse(0);
    }
}
