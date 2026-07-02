package com.salofresh.service.impl.catalog;

import com.salofresh.constant.CacheNames;
import com.salofresh.dto.catalog.CategoryResponse;
import com.salofresh.entity.Category;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.catalog.CategoryMapper;
import com.salofresh.repository.CategoryRepository;
import com.salofresh.service.catalog.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Cacheable(CacheNames.CATEGORIES)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findAllByActiveTrue().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return categoryMapper.toResponse(category);
    }
}
