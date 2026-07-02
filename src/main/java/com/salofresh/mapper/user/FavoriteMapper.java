package com.salofresh.mapper.user;

import com.salofresh.dto.user.FavoriteSalonResponse;
import com.salofresh.dto.user.RecentlyViewedResponse;
import com.salofresh.entity.RecentlyViewedSalon;
import com.salofresh.entity.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "salonId", source = "id")
    @Mapping(target = "cityName", source = "city.name")
    FavoriteSalonResponse toFavoriteResponse(Salon salon);

    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "name", source = "salon.name")
    @Mapping(target = "slug", source = "salon.slug")
    @Mapping(target = "bannerImageUrl", source = "salon.bannerImageUrl")
    @Mapping(target = "cityName", source = "salon.city.name")
    @Mapping(target = "ratingAverage", source = "salon.ratingAverage")
    @Mapping(target = "reviewCount", source = "salon.reviewCount")
    @Mapping(target = "viewedAt", source = "viewedAt")
    RecentlyViewedResponse toRecentlyViewedResponse(RecentlyViewedSalon recentlyViewedSalon);
}
