package com.salofresh.service.impl.salon;

import com.salofresh.dto.salon.WorkingHoursRequest;
import com.salofresh.dto.salon.WorkingHoursResponse;
import com.salofresh.entity.Salon;
import com.salofresh.entity.WorkingHours;
import com.salofresh.mapper.salon.WorkingHoursMapper;
import com.salofresh.repository.WorkingHoursRepository;
import com.salofresh.service.salon.WorkingHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkingHoursServiceImpl implements WorkingHoursService {

    private final WorkingHoursRepository workingHoursRepository;
    private final WorkingHoursMapper workingHoursMapper;
    private final SalonAccessGuard salonAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<WorkingHoursResponse> list(Long salonId) {
        salonAccessGuard.requireOwnedSalon(salonId);
        return workingHoursMapper.toResponseList(workingHoursRepository.findAllBySalonId(salonId));
    }

    @Override
    @Transactional
    public List<WorkingHoursResponse> bulkUpsert(Long salonId, List<WorkingHoursRequest> requests) {
        Salon salon = salonAccessGuard.requireOwnedSalon(salonId);

        workingHoursRepository.deleteAllBySalonId(salonId);
        List<WorkingHours> entities = requests.stream()
                .map(request -> {
                    WorkingHours workingHours = workingHoursMapper.toEntity(request);
                    workingHours.setSalon(salon);
                    return workingHours;
                })
                .collect(Collectors.toList());

        List<WorkingHours> saved = workingHoursRepository.saveAll(entities);
        return workingHoursMapper.toResponseList(saved);
    }
}
