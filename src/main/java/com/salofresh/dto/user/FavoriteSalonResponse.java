package com.salofresh.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteSalonResponse {

    private Long salonId;
    private String name;
    private String slug;
    private String bannerImageUrl;
    private String cityName;
    private double ratingAverage;
    private int reviewCount;
}
