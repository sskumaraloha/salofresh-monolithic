package com.salofresh.service.salon;

import com.salofresh.dto.salon.SalonCreateRequest;
import com.salofresh.dto.salon.SalonResponse;
import com.salofresh.dto.salon.SalonSearchRequest;
import com.salofresh.dto.salon.SalonSummaryResponse;
import com.salofresh.dto.salon.SalonUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SalonService {

    SalonResponse create(SalonCreateRequest request);

    SalonResponse update(Long salonId, SalonUpdateRequest request);

    void delete(Long salonId);

    SalonResponse pause(Long salonId);

    SalonResponse activate(Long salonId);

    String uploadBanner(Long salonId, MultipartFile file);

    SalonResponse getBySlug(String slug);

    Page<SalonSummaryResponse> search(SalonSearchRequest request);

    List<SalonSummaryResponse> listMySalons();
}
