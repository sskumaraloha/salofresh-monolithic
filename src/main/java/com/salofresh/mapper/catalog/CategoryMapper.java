package com.salofresh.mapper.catalog;

import com.salofresh.dto.catalog.CategoryResponse;
import com.salofresh.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
