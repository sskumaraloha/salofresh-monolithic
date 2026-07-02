package com.salofresh.service.catalog;

import com.salofresh.dto.catalog.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listActive();

    CategoryResponse getById(Long id);
}
