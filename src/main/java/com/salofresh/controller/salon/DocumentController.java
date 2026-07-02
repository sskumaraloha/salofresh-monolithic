package com.salofresh.controller.salon;

import com.salofresh.common.enums.DocumentType;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.DocumentUploadResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.DocumentService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/documents")
@Tag(name = "Salon Documents", description = "Owner-managed compliance documents (business license, GST certificate, etc.) for a salon")
@PreAuthorize("hasRole('SALON_OWNER')")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List documents uploaded for a salon")
    public ResponseEntity<ApiResponse<List<DocumentUploadResponse>>> list(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Documents fetched successfully", documentService.list(salonId)));
    }

    @PostMapping
    @Operation(summary = "Upload a compliance document for a salon")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(@PathVariable Long salonId,
                                                                       @RequestParam DocumentType documentType,
                                                                       @RequestPart("file") MultipartFile file) {
        DocumentUploadResponse response = documentService.upload(salonId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document uploaded successfully", response));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a previously uploaded document")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long documentId) {
        documentService.delete(salonId, documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }
}
