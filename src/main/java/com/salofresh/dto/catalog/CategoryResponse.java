package com.salofresh.dto.catalog;

import com.salofresh.common.enums.ServiceCategoryType;
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
public class CategoryResponse {

    private Long id;
    private String name;
    private ServiceCategoryType type;
    private String description;
    private String imageUrl;
    private boolean active;
}
