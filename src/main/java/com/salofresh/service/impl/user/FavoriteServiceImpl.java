package com.salofresh.service.impl.user;

import com.salofresh.dto.user.FavoriteSalonResponse;
import com.salofresh.dto.user.RecentlyViewedResponse;
import com.salofresh.entity.Favorite;
import com.salofresh.entity.RecentlyViewedSalon;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.user.FavoriteMapper;
import com.salofresh.repository.FavoriteRepository;
import com.salofresh.repository.RecentlyViewedSalonRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.user.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RecentlyViewedSalonRepository recentlyViewedSalonRepository;
    private final UserRepository userRepository;
    private final SalonRepository salonRepository;
    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FavoriteSalonResponse> listFavorites(Long userId, Pageable pageable) {
        Page<Favorite> page = favoriteRepository.findAllByUserId(userId, pageable);
        List<FavoriteSalonResponse> content = page.getContent().stream()
                .map(favorite -> favoriteMapper.toFavoriteResponse(favorite.getSalon()))
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional
    public void addFavorite(Long userId, Long salonId) {
        if (favoriteRepository.existsByUserIdAndSalonId(userId, salonId)) {
            throw new ConflictException("Salon is already in favorites");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Salon salon = getExistingSalon(salonId);

        Favorite favorite = Favorite.builder()
                .user(user)
                .salon(salon)
                .build();
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long salonId) {
        if (!favoriteRepository.existsByUserIdAndSalonId(userId, salonId)) {
            throw new ResourceNotFoundException("Favorite", "salonId", salonId);
        }
        favoriteRepository.deleteByUserIdAndSalonId(userId, salonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewedResponse> listRecentlyViewed(Long userId, Pageable pageable) {
        return recentlyViewedSalonRepository.findAllByUserIdOrderByViewedAtDesc(userId, pageable).stream()
                .map(favoriteMapper::toRecentlyViewedResponse)
                .toList();
    }

    @Override
    @Transactional
    public void recordView(Long userId, Long salonId) {
        RecentlyViewedSalon recentlyViewed = recentlyViewedSalonRepository.findByUserIdAndSalonId(userId, salonId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    Salon salon = getExistingSalon(salonId);
                    return RecentlyViewedSalon.builder()
                            .user(user)
                            .salon(salon)
                            .build();
                });
        recentlyViewed.setViewedAt(Instant.now());
        recentlyViewedSalonRepository.save(recentlyViewed);
    }

    private Salon getExistingSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.isDeleted()) {
            throw new ResourceNotFoundException("Salon", "id", salonId);
        }
        return salon;
    }
}
