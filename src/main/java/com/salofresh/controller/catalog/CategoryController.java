package com.salofresh.controller.catalog;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.catalog.CategoryResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.catalog.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Fixed, admin-managed salon service categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List all active service categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", categoryService.listActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get a category by id")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Category fetched successfully", categoryService.getById(id)));
    }
}
