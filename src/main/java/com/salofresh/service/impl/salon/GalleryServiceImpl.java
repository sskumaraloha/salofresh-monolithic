package com.salofresh.service.impl.salon;

import com.salofresh.dto.salon.GalleryImageResponse;
import com.salofresh.entity.Gallery;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.mapper.salon.GalleryMapper;
import com.salofresh.repository.GalleryRepository;
import com.salofresh.service.salon.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GalleryServiceImpl implements GalleryService {

    private static final String GALLERY_SUB_DIRECTORY = "salon-gallery";

    private final GalleryRepository galleryRepository;
    private final GalleryMapper galleryMapper;
    private final FileStorageService fileStorageService;
    private final SalonAccessGuard salonAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<GalleryImageResponse> list(Long salonId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        return galleryMapper.toResponseList(galleryRepository.findAllBySalonIdOrderBySortOrderAsc(salonId));
    }

    @Override
    @Transactional
    public GalleryImageResponse upload(Long salonId, MultipartFile file, String caption) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        String imageUrl = fileStorageService.store(file, GALLERY_SUB_DIRECTORY);

        int nextSortOrder = galleryRepository.findAllBySalonIdOrderBySortOrderAsc(salonId).size();
        Gallery gallery = Gallery.builder()
                .salon(salon)
                .imageUrl(imageUrl)
                .caption(caption)
                .sortOrder(nextSortOrder)
                .build();
        return galleryMapper.toResponse(galleryRepository.save(gallery));
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long imageId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        Gallery gallery = galleryRepository.findByIdAndSalonId(imageId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image", "id", imageId));
        fileStorageService.delete(gallery.getImageUrl());
        galleryRepository.delete(gallery);
    }
}
