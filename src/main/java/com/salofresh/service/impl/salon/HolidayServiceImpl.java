package com.salofresh.service.impl.salon;

import com.salofresh.dto.salon.HolidayRequest;
import com.salofresh.dto.salon.HolidayResponse;
import com.salofresh.entity.Holiday;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.salon.HolidayMapper;
import com.salofresh.repository.HolidayRepository;
import com.salofresh.service.salon.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;
    private final SalonAccessGuard salonAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> list(Long salonId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        return holidayMapper.toResponseList(holidayRepository.findAllBySalonId(salonId));
    }

    @Override
    @Transactional
    public HolidayResponse create(Long salonId, HolidayRequest request) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);
        if (holidayRepository.existsBySalonIdAndHolidayDate(salonId, request.getHolidayDate())) {
            throw new ConflictException("A holiday is already recorded for this salon on %s"
                    .formatted(request.getHolidayDate()));
        }
        Holiday holiday = holidayMapper.toEntity(request);
        holiday.setSalon(salon);
        return holidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    public HolidayResponse update(Long salonId, Long holidayId, HolidayRequest request) {
        salonAccessGuard.requireOwnedSalon(salonId);
        Holiday holiday = holidayRepository.findByIdAndSalonId(holidayId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", holidayId));
        holidayMapper.updateEntityFromRequest(request, holiday);
        return holidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long holidayId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        Holiday holiday = holidayRepository.findByIdAndSalonId(holidayId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", holidayId));
        holidayRepository.delete(holiday);
    }
}
