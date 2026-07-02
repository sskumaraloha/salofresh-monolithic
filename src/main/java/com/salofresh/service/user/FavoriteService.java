package com.salofresh.service.user;

import com.salofresh.dto.user.FavoriteSalonResponse;
import com.salofresh.dto.user.RecentlyViewedResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FavoriteService {

    PagedResponse<FavoriteSalonResponse> listFavorites(Long userId, Pageable pageable);

    void addFavorite(Long userId, Long salonId);

    void removeFavorite(Long userId, Long salonId);

    List<RecentlyViewedResponse> listRecentlyViewed(Long userId, Pageable pageable);

    /**
     * Upserts a "recently viewed" record for the given user/salon pair, setting viewedAt to now.
     * Intended to be called by other modules (e.g. the salon detail page) whenever a customer
     * views a salon.
     */
    void recordView(Long userId, Long salonId);
}
