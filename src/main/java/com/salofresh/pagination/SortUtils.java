package com.salofresh.pagination;

import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortUtils {

    private SortUtils() {
    }

    public static Sort safeSort(String sortBy, String direction, Set<String> allowedFields, String defaultField) {
        String field = (sortBy != null && allowedFields.contains(sortBy)) ? sortBy : defaultField;
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, field);
    }
}
