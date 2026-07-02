package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.GalleryImageResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/gallery")
@Tag(name = "Salon Gallery", description = "Owner-managed photo gallery for a salon")
@PreAuthorize("hasRole('SALON_OWNER')")
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    @Operation(summary = "List gallery images of a salon")
    public ResponseEntity<ApiResponse<List<GalleryImageResponse>>> list(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Gallery images fetched successfully", galleryService.list(salonId)));
    }

    @PostMapping
    @Operation(summary = "Upload a new gallery image for a salon")
    public ResponseEntity<ApiResponse<GalleryImageResponse>> upload(@PathVariable Long salonId,
                                                                     @RequestPart("file") MultipartFile file,
                                                                     @RequestParam(required = false) String caption) {
        GalleryImageResponse response = galleryService.upload(salonId, file, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Gallery image uploaded successfully", response));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete a gallery image")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long imageId) {
        galleryService.delete(salonId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Gallery image deleted successfully"));
    }
}
