package com.salofresh.service.impl.salon;

import com.salofresh.common.enums.EntityStatus;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.constant.CacheNames;
import com.salofresh.dto.salon.SalonCreateRequest;
import com.salofresh.dto.salon.SalonResponse;
import com.salofresh.dto.salon.SalonSearchRequest;
import com.salofresh.dto.salon.SalonSummaryResponse;
import com.salofresh.dto.salon.SalonUpdateRequest;
import com.salofresh.entity.City;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonOwner;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.salon.GalleryMapper;
import com.salofresh.mapper.salon.HolidayMapper;
import com.salofresh.mapper.salon.SalonMapper;
import com.salofresh.mapper.salon.WorkingHoursMapper;
import com.salofresh.repository.CityRepository;
import com.salofresh.repository.GalleryRepository;
import com.salofresh.repository.HolidayRepository;
import com.salofresh.repository.SalonOwnerRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.WorkingHoursRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.salon.SalonService;
import com.salofresh.util.GeoUtils;
import com.salofresh.util.SlugUtils;
import com.salofresh.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.salofresh.specification.SalonSpecification.hasCity;
import static com.salofresh.specification.SalonSpecification.hasGenderType;
import static com.salofresh.specification.SalonSpecification.hasMaxPrice;
import static com.salofresh.specification.SalonSpecification.hasMinRating;
import static com.salofresh.specification.SalonSpecification.isApprovedAndActive;

@Service
@RequiredArgsConstructor
public class SalonServiceImpl implements SalonService {

    private static final String BANNER_SUB_DIRECTORY = "salon-banners";

    private final SalonRepository salonRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final CityRepository cityRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final HolidayRepository holidayRepository;
    private final GalleryRepository galleryRepository;
    private final SalonMapper salonMapper;
    private final WorkingHoursMapper workingHoursMapper;
    private final HolidayMapper holidayMapper;
    private final GalleryMapper galleryMapper;
    private final SecurityUtils securityUtils;
    private final FileStorageService fileStorageService;
    private final SalonAccessGuard salonAccessGuard;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public SalonResponse create(SalonCreateRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        SalonOwner owner = salonOwnerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("SalonOwner", "userId", currentUserId));
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));

        Salon salon = salonMapper.toEntity(request);
        salon.setOwner(owner);
        salon.setCity(city);
        salon.setSlug(generateUniqueSlug(request.getName()));
        salon.setSlotDurationMinutes(request.getSlotDurationMinutes() != null
                ? request.getSlotDurationMinutes() : AppConstants.DEFAULT_SLOT_DURATION_MINUTES);
        salon.setBufferTimeMinutes(request.getBufferTimeMinutes() != null
                ? request.getBufferTimeMinutes() : AppConstants.DEFAULT_BUFFER_TIME_MINUTES);
        salon.setMaxBookingsPerSlot(request.getMaxBookingsPerSlot() != null ? request.getMaxBookingsPerSlot() : 1);

        salon = salonRepository.save(salon);
        return toFullResponse(salon);
    }

    @Override
    @Transactional
    public SalonResponse update(Long salonId, SalonUpdateRequest request) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.getCityId()));

        salonMapper.updateEntityFromRequest(request, salon);
        salon.setCity(city);
        if (request.getSlotDurationMinutes() != null) {
            salon.setSlotDurationMinutes(request.getSlotDurationMinutes());
        }
        if (request.getBufferTimeMinutes() != null) {
            salon.setBufferTimeMinutes(request.getBufferTimeMinutes());
        }
        if (request.getMaxBookingsPerSlot() != null) {
            salon.setMaxBookingsPerSlot(request.getMaxBookingsPerSlot());
        }

        salon = salonRepository.save(salon);
        evictSalonDetailCache(salon.getSlug());
        return toFullResponse(salon);
    }

    @Override
    @Transactional
    public void delete(Long salonId) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        salon.setDeleted(true);
        salonRepository.save(salon);
        evictSalonDetailCache(salon.getSlug());
    }

    @Override
    @Transactional
    public SalonResponse pause(Long salonId) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        salon.setStatus(SalonStatus.PAUSED);
        salon = salonRepository.save(salon);
        evictSalonDetailCache(salon.getSlug());
        return toFullResponse(salon);
    }

    @Override
    @Transactional
    public SalonResponse activate(Long salonId) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        salon.setStatus(SalonStatus.ACTIVE);
        salon = salonRepository.save(salon);
        evictSalonDetailCache(salon.getSlug());
        return toFullResponse(salon);
    }

    @Override
    @Transactional
    public String uploadBanner(Long salonId, MultipartFile file) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        String previousBanner = salon.getBannerImageUrl();
        String bannerUrl = fileStorageService.store(file, BANNER_SUB_DIRECTORY);
        salon.setBannerImageUrl(bannerUrl);
        salonRepository.save(salon);
        evictSalonDetailCache(salon.getSlug());
        if (previousBanner != null && !previousBanner.isBlank()) {
            fileStorageService.delete(previousBanner);
        }
        return bannerUrl;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.SALON_DETAILS, key = "#slug")
    @Transactional(readOnly = true)
    public SalonResponse getBySlug(String slug) {
        Salon salon = salonRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "slug", slug));
        return toFullResponse(salon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalonSummaryResponse> search(SalonSearchRequest request) {
        Specification<Salon> specification = Specification.where(isApprovedAndActive())
                .and(hasCity(request.getCity()))
                .and(hasMinRating(request.getMinRating()))
                .and(hasGenderType(request.getGenderType()))
                .and(hasMaxPrice(request.getMaxPrice()));

        boolean sortByDistance = "distance".equalsIgnoreCase(request.getSortBy());
        Pageable pageable = buildPageable(request, sortByDistance);

        Page<Salon> page = salonRepository.findAll(specification, pageable);

        List<SalonSummaryResponse> content = page.getContent().stream()
                .map(salon -> toSummaryResponse(salon, request.getLatitude(), request.getLongitude()))
                .collect(Collectors.toList());

        if (sortByDistance && request.getLatitude() != null && request.getLongitude() != null) {
            content.sort(Comparator.comparing(SalonSummaryResponse::getDistanceKm,
                    Comparator.nullsLast(Double::compareTo)));
        }

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonSummaryResponse> listMySalons() {
        Long currentUserId = securityUtils.getCurrentUserId();
        SalonOwner owner = salonOwnerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("SalonOwner", "userId", currentUserId));
        return salonRepository.findAllByOwnerIdAndDeletedFalse(owner.getId()).stream()
                .map(salon -> toSummaryResponse(salon, null, null))
                .collect(Collectors.toList());
    }

    private Pageable buildPageable(SalonSearchRequest request, boolean sortByDistance) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 20 : request.getSize();
        if (sortByDistance) {
            // distance is computed in-memory after fetch; fall back to rating for the DB-level order
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ratingAverage"));
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = request.getSortBy() == null || request.getSortBy().isBlank() ? "ratingAverage" : request.getSortBy();
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private String generateUniqueSlug(String name) {
        String base = SlugUtils.toSlug(name);
        if (base.isBlank()) {
            base = "salon";
        }
        String candidate = base;
        int suffix = 1;
        while (salonRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private void evictSalonDetailCache(String slug) {
        var cache = cacheManager.getCache(CacheNames.SALON_DETAILS);
        if (cache != null) {
            cache.evict(slug);
        }
    }

    private BigDecimal resolveStartingPrice(Long salonId) {
        return salonServiceRepository.findAllBySalonIdAndDeletedFalse(salonId).stream()
                .filter(service -> service.getStatus() == EntityStatus.ACTIVE)
                .map(com.salofresh.entity.SalonService::getEffectivePrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private SalonResponse toFullResponse(Salon salon) {
        SalonResponse response = salonMapper.toResponse(salon);
        response.setGalleryImages(galleryMapper.toResponseList(galleryRepository.findAllBySalonIdOrderBySortOrderAsc(salon.getId())));
        response.setWorkingHours(workingHoursMapper.toResponseList(workingHoursRepository.findAllBySalonId(salon.getId())));
        response.setHolidays(holidayMapper.toResponseList(holidayRepository.findAllBySalonId(salon.getId())));
        return response;
    }

    private SalonSummaryResponse toSummaryResponse(Salon salon, Double fromLatitude, Double fromLongitude) {
        SalonSummaryResponse summary = salonMapper.toSummary(salon);
        summary.setStartingPrice(resolveStartingPrice(salon.getId()));
        if (fromLatitude != null && fromLongitude != null && salon.getLatitude() != null && salon.getLongitude() != null) {
            summary.setDistanceKm(GeoUtils.distanceInKm(fromLatitude, fromLongitude, salon.getLatitude(), salon.getLongitude()));
        }
        return summary;
    }
}
