package com.salofresh.file;

import com.salofresh.constant.AppConstants;
import com.salofresh.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Generic file upload for images and documents")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/{category}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload an image or document into the given category folder")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(@PathVariable String category,
                                                                   @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, category);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully",
                FileUploadResponse.builder().fileUrl(url).build()));
    }
}
