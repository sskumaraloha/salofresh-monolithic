package com.salofresh.service.salon;

import com.salofresh.dto.salon.GalleryImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GalleryService {

    List<GalleryImageResponse> list(Long salonId);

    GalleryImageResponse upload(Long salonId, MultipartFile file, String caption);

    void delete(Long salonId, Long imageId);
}
