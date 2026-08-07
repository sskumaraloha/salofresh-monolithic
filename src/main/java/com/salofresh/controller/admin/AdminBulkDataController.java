package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.bulk.BulkImportResult;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.bulk.BulkDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/bulk")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Bulk Data", description = "Admin-only bulk CSV export/import for core entities")
public class AdminBulkDataController {

    private final BulkDataService bulkDataService;

    @GetMapping("/categories/export")
    @Operation(summary = "Export all categories as a CSV file (Content-Type: text/csv)")
    public ResponseEntity<byte[]> exportCategories() {
        return csvResponse(bulkDataService.exportCategoriesCsv(), "categories.csv");
    }

    @GetMapping("/salons/export")
    @Operation(summary = "Export all salons as a CSV file (Content-Type: text/csv)")
    public ResponseEntity<byte[]> exportSalons() {
        return csvResponse(bulkDataService.exportSalonsCsv(), "salons.csv");
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export all users as a CSV file, excluding sensitive fields (Content-Type: text/csv)")
    public ResponseEntity<byte[]> exportUsers() {
        return csvResponse(bulkDataService.exportUsersCsv(), "users.csv");
    }

    @PostMapping("/categories/import")
    @Operation(summary = "Bulk import categories from a CSV file (columns: name,type,description,active)")
    public ResponseEntity<ApiResponse<BulkImportResult>> importCategories(@RequestParam("file") MultipartFile file) {
        BulkImportResult result = bulkDataService.importCategories(file);
        return ResponseEntity.ok(ApiResponse.success("Categories import processed", result));
    }

    private ResponseEntity<byte[]> csvResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }
}
