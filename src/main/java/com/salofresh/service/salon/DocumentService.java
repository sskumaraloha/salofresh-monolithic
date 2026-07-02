package com.salofresh.service.salon;

import com.salofresh.common.enums.DocumentType;
import com.salofresh.dto.salon.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<DocumentUploadResponse> list(Long salonId);

    DocumentUploadResponse upload(Long salonId, DocumentType documentType, MultipartFile file);

    void delete(Long salonId, Long documentId);
}
