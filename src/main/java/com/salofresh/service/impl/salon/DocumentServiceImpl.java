package com.salofresh.service.impl.salon;

import com.salofresh.common.enums.DocumentType;
import com.salofresh.dto.salon.DocumentUploadResponse;
import com.salofresh.entity.Document;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.repository.DocumentRepository;
import com.salofresh.service.salon.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String DOCUMENT_SUB_DIRECTORY = "salon-documents";

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final SalonAccessGuard salonAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentUploadResponse> list(Long salonId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        return documentRepository.findAllBySalonId(salonId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocumentUploadResponse upload(Long salonId, DocumentType documentType, MultipartFile file) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        String fileUrl = fileStorageService.store(file, DOCUMENT_SUB_DIRECTORY);

        Document document = Document.builder()
                .salon(salon)
                .documentType(documentType)
                .fileUrl(fileUrl)
                .verified(false)
                .build();
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long documentId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        Document document = documentRepository.findByIdAndSalonId(documentId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        fileStorageService.delete(document.getFileUrl());
        documentRepository.delete(document);
    }

    private DocumentUploadResponse toResponse(Document document) {
        return DocumentUploadResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .fileUrl(document.getFileUrl())
                .verified(document.isVerified())
                .build();
    }
}
